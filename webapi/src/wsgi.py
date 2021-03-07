"""Wrapper of our flask app for gunicorn"""

from flaskapp import app as application

if __name__ == "__main__":
    application.run()
