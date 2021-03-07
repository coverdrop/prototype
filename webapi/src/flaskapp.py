from flask import Flask, jsonify, request, g
from flask_httpauth import HTTPTokenAuth
from functools import wraps
from werkzeug.security import generate_password_hash, check_password_hash

import datastore

app = Flask(__name__)
auth = HTTPTokenAuth(scheme='Token')

# This is a demonstration prototype; hence we use very simple
# tokens and do not do any hashing
token_to_service = {
    "news_app_token": "news_app",
    "reporter_app_token": "reporter_app",
    "sgx_token": "sgx",
}


@auth.verify_token
def verify_token(token):
    if token in token_to_service:
        g.service_name = token_to_service[token]
        return True
    return False


def require_service_auth(require):
    """Throws an authentication error if the token is wrong or the authenticated
    service is not the one specified via `require`.
    """
    def decorator(func):
        @wraps(func)
        def decorated(*args, **kwargs):
            auth_wrapped = auth.login_required(func)(*args, **kwargs)
            if 'service_name' not in g:
                print("AUTH FAILED: invalid or missing token")
                return auth_wrapped

            if g.service_name == require:
                print("AUTH PASSED: %s" % g.service_name)
                return auth_wrapped
            else:
                print("AUTH FAILED: wrong service: got '%s' but required '%s'" %
                      (g.service_name, require))
                return auth.auth_error_callback()

        return decorated
    return decorator

#
# Routes for providing the news app content such as stories, reporters, and
# dead drop messages
#


@app.route('/')
@require_service_auth(require='news_app')
def get_index():
    stories = datastore.get_all_news_stories()
    stories_dicts = [it.to_dict() for it in stories]
    return jsonify(stories_dicts)


@app.route('/story/<id>')
@require_service_auth(require='news_app')
def get_story(id):
    story = datastore.get_news_story(id=id)
    return jsonify(story.to_dict())


@app.route('/reporters')
@require_service_auth(require='news_app')
def get_reporters():
    reporters = datastore.get_all_reporters()
    reporters_dict = [it.to_dict() for it in reporters]
    return jsonify(reporters_dict)


#
# Routes concerning key distribution
#

@app.route('/pubkeys')
@require_service_auth(require='news_app')
def get_pub_keys():
    reporters = datastore.get_all_reporters()
    reporter_keys = {v.id: v.pub_key for v in reporters}
    return jsonify({
        'sgx_key': datastore.get_sgx_key(),
        'sgx_sign_key': datastore.get_sgx_sign_key(),
        'reporter_keys': reporter_keys,
    })


#
# Routes concerning communication with user and reporter messages
#
# The order of methods follows a standard scenario:
#  - User sends a message
#  - The SGX queries all buffered user messages
#  - The user message is deleted
#  - The SGX posts a new message to be forwarded to a reporter
#  - The reporter posts a message (e.g. a reply)
#  - The SGX queries all buffered reporter messages
#  - The reporter message is deleted
#  - The SGX posts a new message to be forwarded to a user
#


@app.route('/user_message', methods=['POST'])
@require_service_auth(require='news_app')
def post_user_message():
    """Called from the user app to post a new message."""

    message = request.json['message']
    datastore.add_user_message(message)
    return ""


@app.route('/user_messages')
@require_service_auth(require='sgx')
def get_user_messages():
    """Called from the SGX to query the oldest `count` user messages."""
    count = request.args.get('count')
    messages = datastore.get_user_messages(count)
    messages_array = [m.to_dict() for m in messages]
    return jsonify(messages_array)


@app.route('/user_message/<id>', methods=['DELETE'])
@require_service_auth(require='sgx')
def delete_user_message(id):
    """"Called from the SGX to delete the user message
    with the given `id`.
    """
    datastore.delete_user_message(id)
    return ""


@app.route('/send_to_reporter', methods=['POST'])
@require_service_auth(require='sgx')
def post_send_to_reporter():
    """Called from the SGX to send a message to the reporters
    """
    messages = request.json['messages']

    for message in messages:
        datastore.add_reporter_inbox_message(message)
    return ""


@app.route('/reporter_inbox', methods=['GET'])
@require_service_auth(require='reporter_app')
def get_reporter_inbox():
    """Called from the reporter app to get recent messages."""
    messages = datastore.get_active_reporter_inbox_messages()
    return jsonify(messages)


@app.route('/reporter_message', methods=['POST'])
@require_service_auth(require='reporter_app')
def post_from_reporter():
    """Called from the reporter app to post a new reporter message."""
    message = request.json['message']
    datastore.add_reporter_message(message)
    return ""


@app.route('/reporter_messages')
@require_service_auth(require='sgx')
def get_reporter_messages():
    """Called from the SGX to query the oldest `count`
    reporter messages.
    """
    count = request.args.get('count')
    messages = datastore.get_reporter_messages(count)
    messages_array = [m.to_dict() for m in messages]
    return jsonify(messages_array)


@app.route('/reporter_message/<id>', methods=['DELETE'])
@require_service_auth(require='sgx')
def delete_reporter_message(id):
    """"Called from the SGX to delete the reporter message
    with the given `id`.
    """
    datastore.delete_reporter_message(id)
    return ""


@app.route('/send_to_users', methods=['POST'])
@require_service_auth(require='sgx')
def post_send_to_users():
    """Called from the SGX to add the messages to the list of messages that
    are to be distributed to the user apps.
    """
    messages = request.json['messages']

    for message in messages:
        datastore.add_dead_drop_message(message)
    return ""


@app.route('/deaddrop')
@require_service_auth(require='news_app')
def get_dead_drop_messages():
    messages = datastore.get_active_dead_drop_messages()
    return jsonify(messages)


#
# Debugging endpoints
#


@app.route('/debug/all_messages', methods=['GET'])
@require_service_auth(require='sgx')
def get_debug_all_messages():
    result = {
        "user_messages": [m.to_dict() for m in datastore.get_user_messages(9999)],
        "reporter_inboxes": datastore.get_active_reporter_inbox_messages(),
        "reporter_messages": [m.to_dict() for m in datastore.get_reporter_messages(9999)],
        "deaddrop": datastore.get_active_dead_drop_messages()
    }
    return jsonify(result)


@app.route('/debug/delete_all_messages', methods=['POST'])
@require_service_auth(require='sgx')
def post_delete_all_messages():
    datastore.delete_all_messages()
    return "All messages deleted\n"
