# WebApi

Point of contact for this folder: [@lambdapioneer](https://github.com/lambdapioneer)

This folder contains a simple Flask app that implements the web service that decouples the external API and the SGX. It also provides some basic APIs for simulated news content.

## Setup

This project follows the classic virtualenv/venv with pip configuration. To get started, ensure you have `venv` and `pip` installed on your machine. Then run the following within this folder:

```
$ python3 -m venv env
$ source env/bin/activate
(env) $ pip install -r requirements.txt
```


## Running the webservice

The following command will launch the `flask`-based webservice in debug mode:

```
(env) $ env FLASK_APP=src/flaskapp.py FLASK_ENV=development flask run
```

For the actual deployment on our virtual machine we use `gunicorn` as an application server behind nginx. Run the following from within the `src` folder:

```
(env) $ gunicorn --bind 0.0.0.0:8000 wsgi
```


## Modifying the back-end storage

You can use the following helpers (for now) within the `venv` to modify the back-end storage of news stories. They are always created from "Lorem Ipsum" sample text. These commands are safe to run while the web service is running:

```
(enc) $ cd src
(env) $ python3 cli.py --help
usage: CoverDrop CLI [-h]
                     {story_add,stories_clear,reporter_add,reporter_list,clear_and_default}
                     ...

positional arguments:
  {story_add,stories_clear,reporter_add,reporter_list,clear_and_default}
    story_add           Adds a random news story
    stories_clear       Removes all news stories
    reporter_add        Adds a new reporter with a given name and address
    reporter_list       Lists all reporters
    clear_and_default   Clears the entire DB and generates a scenario with
                        default reporters and articles

optional arguments:
  -h, --help            show this help message and exit
```

## API

Check out the main documentation for detailed API information. For a quick check of your setup use the following commands which at least should return an empty `200 OK` response:

For your local setup:

`curl -i -H "Authorization: Token news_app_token" -X GET 127.0.0.1:5000/`

For the production setup:

`curl -i -H "Authorization: Token news_app_token" -X GET http://coverdrop.cl.cam.ac.uk/`
