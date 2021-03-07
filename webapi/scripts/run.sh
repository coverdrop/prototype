#!/bin/bash
source env/bin/activate;
python3 -mpip install -r requirements.txt;
cd src;
killall python3;
gunicorn --bind 0.0.0.0:8000 wsgi;
