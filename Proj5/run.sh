#!/bin/bash
chmod 777 ./mysite/static/image
python3 manage.py makemigrations
python3 manage.py migrate
python3 manage.py loaddata mydata.json
python3 manage.py loaddata mydata1.json
python3 manage.py loaddata mydata2.json
python3 manage.py loaddata superuser.json
python3 manage.py runserver 0.0.0.0:8000
