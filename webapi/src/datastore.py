from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

import datetime
import os
import sys

Base = declarative_base()
__has_init = False


def _get_engine():
    return create_engine('sqlite:///coverdrop.sqlite', echo=False)


def init():
    global __has_init
    if not __has_init:
        Base.metadata.create_all(_get_engine())
        __has_init = True


def delete_all():
    with ScopeSession() as session:
        session.query(NewsStory).delete()
        session.query(Reporter).delete()
        session.query(UserMessage).delete()
        session.query(ReporterMessage).delete()
        session.query(ReporterInboxMessage).delete()
        session.query(DeadDropMessage).delete()
        session.commit()


def delete_all_messages():
    with ScopeSession() as session:
        session.query(UserMessage).delete()
        session.query(ReporterMessage).delete()
        session.query(ReporterInboxMessage).delete()
        session.query(DeadDropMessage).delete()
        session.commit()


class ScopeSession:

    def __enter__(self):
        init()
        self.session = sessionmaker(bind=_get_engine())()
        return self.session

    def __exit__(self, type, value, traceback):
        self.session.close()


#
# News stories (i.e. the regular news stories)
#


class NewsStory(Base):
    __tablename__ = 'stories'

    id = Column(Integer, primary_key=True)
    headline = Column(String)
    reporter = Column(Integer, ForeignKey('reporters.id'))
    content = Column(String)
    image = Column(String)

    def to_dict(self):
        reporter = get_reporter(id=self.reporter)
        return {
            'id': self.id,
            'headline': self.headline,
            'content': self.content,
            'image': self.image,
            'reporter': reporter.to_dict()
        }


def get_all_news_stories():
    with ScopeSession() as session:
        return session.query(NewsStory).all()


def get_news_story(id):
    with ScopeSession() as session:
        return session.query(NewsStory).filter(NewsStory.id == id).one()


def add_news_story(story):
    with ScopeSession() as session:
        session.add(story)
        session.commit()


def delete_all_news_stories():
    with ScopeSession() as session:
        session.query(NewsStory).delete()
        session.commit()


#
# Reporters (i.e. the investigative journalists)
#

class Reporter(Base):
    __tablename__ = 'reporters'

    id = Column(Integer, primary_key=True)
    name = Column(String)
    pub_key = Column(String)
    image = Column(String)

    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'pub_key': self.pub_key,
            'image': self.image,
        }


def add_reporter(reporter):
    with ScopeSession() as session:
        session.add(reporter)
        session.commit()


def get_all_reporters():
    with ScopeSession() as session:
        return session.query(Reporter).all()


def get_reporter(id):
    with ScopeSession() as session:
        return session.query(Reporter).filter(Reporter.id == id).one()


def delete_all_reporters():
    with ScopeSession() as session:
        session.query(Reporter).delete()
        session.commit()

#
# UserMessages that users have sent and that the SGX will pull
#


class UserMessage(Base):
    __tablename__ = 'usermessages'

    id = Column(Integer, primary_key=True)
    message = Column(String)

    def to_dict(self):
        return {'id': self.id, 'message': self.message}


def add_user_message(message):
    with ScopeSession() as session:
        session.add(UserMessage(message=message))
        session.commit()


def get_user_messages(count):
    with ScopeSession() as session:
        return session.query(UserMessage).order_by(UserMessage.id).limit(count).all()


def delete_user_message(id):
    with ScopeSession() as session:
        session.query(UserMessage).filter(UserMessage.id == id).delete()
        session.commit()

#
# ReporterMessages that reporters have sent and that the SGX will pull
#


class ReporterMessage(Base):
    __tablename__ = 'reportermessages'

    id = Column(Integer, primary_key=True, autoincrement=True)
    message = Column(String)

    def to_dict(self):
        return {'id': self.id, 'message': self.message}


def add_reporter_message(message):
    with ScopeSession() as session:
        session.add(ReporterMessage(message=message))
        session.commit()


def get_reporter_messages(count):
    with ScopeSession() as session:
        return session.query(ReporterMessage).order_by(ReporterMessage.id).limit(count).all()


def delete_reporter_message(id):
    with ScopeSession() as session:
        session.query(ReporterMessage).filter(
            ReporterMessage.id == id).delete()
        session.commit()


#
# DeadDropMessages (i.e. messages to be fed back to the users)
#

class DeadDropMessage(Base):
    __tablename__ = 'deaddropmessages'

    id = Column(Integer, primary_key=True)
    message = Column(String)
    creation_datetime = Column(DateTime)


def add_dead_drop_message(message):
    with ScopeSession() as session:
        session.add(DeadDropMessage(
            message=message,
            creation_datetime=datetime.datetime.now()
        ))
        session.commit()


def get_active_dead_drop_messages(last_n_hours=24):
    with ScopeSession() as session:
        cutoff_datetime = datetime.datetime.now() - datetime.timedelta(hours=last_n_hours)
        messages = session.query(DeadDropMessage).filter(
            DeadDropMessage.creation_datetime > cutoff_datetime).all()

        return [m.message for m in messages]


#
# ReporterInboxMessage (i.e. messages read by the reporter app)
#

class ReporterInboxMessage(Base):
    __tablename__ = 'reporterinboxmessages'

    id = Column(Integer, primary_key=True)
    message = Column(String)
    creation_datetime = Column(DateTime)


def add_reporter_inbox_message(message):
    with ScopeSession() as session:
        session.add(ReporterInboxMessage(
            message=message,
            creation_datetime=datetime.datetime.now()
        ))
        session.commit()


def get_active_reporter_inbox_messages(last_n_hours=24):
    with ScopeSession() as session:
        cutoff_datetime = datetime.datetime.now() - datetime.timedelta(hours=last_n_hours)
        messages = session.query(ReporterInboxMessage).filter(
            (ReporterInboxMessage.creation_datetime > cutoff_datetime)
        ).all()

        return [m.message for m in messages]

#
# Public keys
#

def get_sgx_key():
    with open(os.path.join('keys', 'sgx_key.hex'), 'r') as f:
        return f.readline()

def get_sgx_sign_key():
    with open(os.path.join('keys', 'sgx_sign_key.hex'), 'r') as f:
        return f.readline()

def get_reporter_key(reporter_id):
    with open(os.path.join('keys', 'reporter_%d_key.hex' % reporter_id), 'r') as f:
        return f.readline()
