#!/bin/bash
./scripts/clean.sh;
rsync -rv requirements.txt src scripts $COVERDROP_USER@$COVERDROP_HOST:~/coverdrop;
rsync -rv keys $COVERDROP_USER@$COVERDROP_HOST:~/coverdrop/src;
