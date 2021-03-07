#!/bin/bash
set -e;
BASE_URL=$1;

if [ $# -lt 1 ]; then
  echo 1>&2 "$0: Missing baseUrl argument. Usage examples:";
  echo "     ./curl_all_endpoints.sh 127.0.0.1:5000";
  echo "     ./curl_all_endpoints.sh coverdrop.cl.cam.ac.uk";
  exit 1;
fi

#
# General public endpoints
#
echo "-> Get all news stories (muted)";
echo -n "Reponse body length: ";
curl --fail -s -H "Authorization: Token news_app_token" -X GET $BASE_URL/ | wc -c;

echo "-> Get first news story (muted)";
echo -n "Reponse body length: ";
curl --fail -s -H "Authorization: Token news_app_token" -X GET $BASE_URL/story/1 | wc -c;

echo "-> Get reporters (muted)";
echo -n "Reponse body length: ";
curl --fail -s -H "Authorization: Token news_app_token" -X GET $BASE_URL/reporters | wc -c;

echo "-> Get public keys";
curl --fail -s -H "Authorization: Token news_app_token" -X GET $BASE_URL/pubkeys;

#
# Follow a message through the entire path from user to reporter and back again
#
echo "-> News app sends a message";
curl --fail -H "Authorization: Token news_app_token" -H "Content-Type: application/json" -X POST \
    -d '{"message": "AAAAAA"}' $BASE_URL/user_message;

echo "-> SGX gets user messages";
curl --fail -s -H "Authorization: Token sgx_token" -X GET $BASE_URL/user_messages?count=10;

echo "-> SGX deletes user message";
curl --fail -H "Authorization: Token sgx_token" -X DELETE $BASE_URL/user_message/1;

echo "-> SGX posts message to reporter";
curl --fail -H "Authorization: Token sgx_token" -H "Content-Type: application/json" -X POST \
    -d '{"messages": ["BBBBBB01", "BBBBBB02"]}' $BASE_URL/send_to_reporter;

echo "-> Reporter app gets reporter inbox";
curl --fail -s -H "Authorization: Token reporter_app_token" -X GET $BASE_URL/reporter_inbox;

echo "-> Reporter app sends message";
curl --fail -s -H "Authorization: Token reporter_app_token" -H "Content-Type: application/json" -X POST \
    -d '{"message": "CCCCCC"}' $BASE_URL/reporter_message;

echo "-> SGX gets reporter message";
curl --fail -s -H "Authorization: Token sgx_token" -X GET $BASE_URL/reporter_messages?count=10 ;

echo "-> SGX deletes reporter message";
curl --fail -H "Authorization: Token sgx_token" -X DELETE $BASE_URL/reporter_message/1;

echo "-> SGX posts message to user";
curl --fail -H "Authorization: Token sgx_token" -H "Content-Type: application/json" -X POST \
    -d '{"messages": ["DDDDDD01", "DDDDDD02"]}' $BASE_URL/send_to_users;

echo "-> News app gets deaddrop (muted)";
curl --fail -s -H "Authorization: Token news_app_token" -X GET $BASE_URL/deaddrop ;

echo "[+] All endpoints appear to be up and not throwing"