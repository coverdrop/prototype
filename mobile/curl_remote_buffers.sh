#!/bin/bash
set -e;
BASE_URL="coverdrop.cl.cam.ac.uk";

echo "[ ] /user_messages?count=10";
curl --fail -s -H "Authorization: Token sgx_token" -X GET $BASE_URL/user_messages?count=10 | jq;

echo "[ ] /reporter_inbox/1";
curl --fail -s -H "Authorization: Token reporter_app_token" -X GET $BASE_URL/reporter_inbox/1 | jq;

echo "[ ] /reporter_inbox/2";
curl --fail -s -H "Authorization: Token reporter_app_token" -X GET $BASE_URL/reporter_inbox/2 | jq;

echo "[ ] /reporter_messages?count=10";
curl --fail -s -H "Authorization: Token sgx_token" -X GET $BASE_URL/reporter_messages?count=10 | jq;

echo "[ ] /deaddrop";
curl --fail -s -H "Authorization: Token news_app_token" -X GET $BASE_URL/deaddrop | jq;

echo "[+] done :)";
