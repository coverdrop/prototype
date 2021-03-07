#!/bin/bash
./scripts/deploy.sh;
ssh $COVERDROP_USER@$COVERDROP_HOST -t "cd /home/dh623/coverdrop/ && ./scripts/run.sh"
