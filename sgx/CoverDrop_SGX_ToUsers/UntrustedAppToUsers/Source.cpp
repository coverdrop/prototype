#include <stdio.h>
#include <tchar.h>
#include <string>
#include <stdlib.h>
#include <iostream>
#include <boost/beast/core.hpp>
#include <boost/beast/http.hpp>
#include <boost/beast/version.hpp>
#include <boost/asio/connect.hpp>
#include <boost/asio/ip/tcp.hpp>
#include <boost/lexical_cast.hpp>
#include <cstdlib>
#include "json.hpp"
#include "sgx_urts.h"
#include "CoverDrop_SGX_ToUsers_u.h"
#define ENCLAVE_FILE _T("CoverDrop_SGX_ToUsers.signed.dll")
#define MAX_BUF_LEN 100

namespace beast = boost::beast;     // from <boost/beast.hpp>
namespace http = beast::http;       // from <boost/beast/http.hpp>
namespace net = boost::asio;        // from <boost/asio.hpp>
using tcp = net::ip::tcp;           // from <boost/asio/ip/tcp.hpp>
using json = nlohmann::json;		// from <json.hpp>

void ocall_print_string(const char *str);
json get_messages(std::string getURL);
sgx_status_t handle_message(sgx_enclave_id_t eid, std::string msg, uint8_t* processedMessages);
int delete_message(std::string delURL);

int main() {
	sgx_enclave_id_t eid;
	sgx_status_t ret = SGX_SUCCESS;
	sgx_status_t innerret = SGX_SUCCESS;
	sgx_launch_token_t token = { 0 };
	int updated = 0;
	// Create the Enclave with above launch token.
	ret = sgx_create_enclave(ENCLAVE_FILE, SGX_DEBUG_FLAG, &token, &updated, &eid, NULL);
	if (ret != SGX_SUCCESS) {
		printf("App: error %#x, failed to create enclave.\n", ret);
		getchar();
		return -1;
	}
	std::string getURL = "/reporter_messages?count=1"; //First, get one message to get length of messages
	uint8_t procmsg = 0;

	json j = get_messages(getURL);
	while (j.size() == 0) {
		printf("No Pending messages on server. Waiting.\n");
		Sleep(10000);
		j = get_messages(getURL);
	}
	json tempj = j.at(0);
	std::string msg = tempj["message"];
	size_t msglen = msg.length();
	printf("Establishing message length as: %d\n", (int)msglen);
	ret = initialize(eid, &innerret, msglen);
	if (ret != SGX_SUCCESS || innerret != SGX_SUCCESS) {
		printf("\nERROR INITIALIZING MESSAGE LENGTH\n");
		getchar();
		return -1;
	}
	ret = clean_array(eid, &innerret);
	if (ret != SGX_SUCCESS || innerret != SGX_SUCCESS) {
		printf("\nERROR CLEANING ARRAYS\n");
		getchar();
		return -1;
	}

	getURL = "/reporter_messages?count=10";
	while (true) {
		j = get_messages(getURL);
		json jarr[MAX_BUF_LEN];
		for (int i = 0; i < j.size(); i++) {
			json temp = j.at(i);
			std::string msg = temp["message"];
			int id = temp["id"];
			ret = handle_message(eid, msg, &procmsg);
			if (ret != SGX_SUCCESS) {
				printf("\nError handling message id: %d\n", id);
				std::cout << procmsg << std::endl;
				continue;
			}
			printf("\nFrom Untrusted app: handled message id %d, currently processed % d messages\n", id, procmsg);

			//Now that you have processed the message, delete it on the server
			std::string delURL = "/reporter_message/" + boost::lexical_cast<std::string>(id);
			int retval = 1;
			retval = delete_message(delURL);
			if (retval == 0)
				printf("\nDeleted message: %d\n", id);
			else
				printf("\nERROR deleting message  %d\n", id);
		}
		printf("\nSleeping before doing another round of get-process-delete\n");
		Sleep(10000); //100 seconds
	}




	// Destroy the enclave when all Enclave calls finished.
	if (SGX_SUCCESS != sgx_destroy_enclave(eid))
		return -1;
	printf("Enter a character before exit ...\n");
	getchar();
	return 0;
}

json get_messages(std::string getURL) {
	json toret;
	try {
		auto const* host = "TODO_CHANGE_ME";
		auto const* port = "80";
		int version = 11;
		// The io_context is required for all I/O
		net::io_context ioc;

		// These objects perform our I/O
		tcp::resolver resolver(ioc);
		beast::tcp_stream stream(ioc);

		// Look up the domain name
		auto const results = resolver.resolve(host, port);

		// Make the connection on the IP address we get from a lookup
		stream.connect(results);

		// Set up an HTTP GET request message
		http::request<http::string_body> req{ http::verb::get, getURL, version };
		req.set(http::field::host, host);
		req.set(http::field::user_agent, BOOST_BEAST_VERSION_STRING);
		req.set(http::field::authorization, "Token sgx_token");

		//std::cout << req << std::endl;
		// Send the HTTP request to the remote host
		http::write(stream, req);

		// This buffer is used for reading and must be persisted
		beast::flat_buffer buffer;

		// Declare a container to hold the response
		http::response<http::string_body> res;

		// Receive the HTTP response
		http::read(stream, buffer, res);

		// Write the message to standard out
		//std::cout << res << std::endl;

		std::string s = res.body();
		json j = json::parse(s);

		// Gracefully close the socket
		beast::error_code ec;
		stream.socket().shutdown(tcp::socket::shutdown_both, ec);

		// not_connected happens sometimes
		// so don't bother reporting it.
		//
		if (ec && ec != beast::errc::not_connected)
			throw beast::system_error{ ec };

		// If we get here then the connection is closed gracefully
		toret = j;
	}
	catch (std::exception const& e) {
		printf(e.what());
	}
	return toret;
}

int delete_message(std::string delURL) {
	int toret = 1;
	try {
		auto const* host = "TODO_CHANGE_ME";
		auto const* port = "80";
		int version = 11;
		// The io_context is required for all I/O
		net::io_context ioc;

		// These objects perform our I/O
		tcp::resolver resolver(ioc);
		beast::tcp_stream stream(ioc);

		// Look up the domain name
		auto const results = resolver.resolve(host, port);

		// Make the connection on the IP address we get from a lookup
		stream.connect(results);

		http::request<http::string_body> req{ http::verb::delete_, delURL, version };
		req.set(http::field::host, host);
		req.set(http::field::user_agent, BOOST_BEAST_VERSION_STRING);
		req.set(http::field::authorization, "Token sgx_token");

		//std::cout << req << std::endl;
		http::write(stream, req);
		beast::flat_buffer buffer;
		http::response<http::string_body> res;
		http::read(stream, buffer, res);

		//std::cout << res << std::endl;
		// Gracefully close the socket
		beast::error_code ec;
		stream.socket().shutdown(tcp::socket::shutdown_both, ec);

		// not_connected happens sometimes
		// so don't bother reporting it.
		//
		if (ec && ec != beast::errc::not_connected)
			throw beast::system_error{ ec };

		// If we get here then the connection is closed gracefully
		toret = 0;
	}
	catch (std::exception const& e) {
		printf(e.what());
	}
	return toret;
}

sgx_status_t handle_message(sgx_enclave_id_t eid, std::string msg, uint8_t* processedMessages) {
	sgx_status_t ret = SGX_SUCCESS;
	sgx_status_t innerret = SGX_SUCCESS;
	char *cstr = &msg[0];
	printf("MESSAGE LEN==%d\n", msg.length());
	ret = process_message(eid, &innerret, cstr, msg.length(), processedMessages);
	if (innerret != SGX_SUCCESS) {
		printf("\nERROR PROCESSING MESSAGE %s\n\n", msg);
		return innerret;
	}
	return ret;
}


/* OCall function to print to console*/
void ocall_print_string(const char *str)
{
	/* Proxy/Bridge will check the length and null-terminate
	* the input string to prevent buffer overflow.
	*/
	printf("%s", str);
}

/* OCall function to send message to reporter*/
int transmit_message_to_users(const char *str, size_t len) {
	int retval = 0;
	try {
		auto const* host = "TODO_CHANGE_ME";
		auto const* port = "80";
		std::string target_user = "/send_to_users";
		int version = 11;
		// The io_context is required for all I/O
		net::io_context ioc;

		// These objects perform our I/O
		tcp::resolver resolver(ioc);
		beast::tcp_stream stream(ioc);

		// Look up the domain name
		auto const results = resolver.resolve(host, port);

		// Make the connection on the IP address we get from a lookup
		stream.connect(results);

		// Set up an HTTP POSt request message
		http::request<http::string_body> req_;
		req_.version(version);
		req_.method(http::verb::post);
		req_.target(target_user);
		req_.set(http::field::host, host);
		req_.set(http::field::authorization, "Token sgx_token");
		req_.set(http::field::content_type, "application/json");
		std::string toSendPre = "{\"messages\": [\"";

		std::string toSend(str);
		std::string toSendPost = "\"]}";
		std::string body = toSendPre + toSend + toSendPost;

		//req_.set(http::field::content_length, body.length());
		req_.body() = body;
		req_.prepare_payload();

		std::cout << req_ << std::endl;
		// Send the HTTP request to the remote host
		http::write(stream, req_);

		// This buffer is used for reading and must be persisted
		beast::flat_buffer buffer;

		// Declare a container to hold the response
		http::response<http::string_body> res;

		// Receive the HTTP response
		http::read(stream, buffer, res);

		// Write the message to standard out
		std::cout << res << std::endl;

		std::string s = res.body();


		std::cout << s << std::endl;


		// Gracefully close the socket
		beast::error_code ec;
		stream.socket().shutdown(tcp::socket::shutdown_both, ec);

		// not_connected happens sometimes
		// so don't bother reporting it.
		//
		if (ec && ec != beast::errc::not_connected)
			throw beast::system_error{ ec };

		// If we get here then the connection is closed gracefully
	}
	catch (std::exception const& e) {
		printf(e.what());
		retval = -1;
	}

	return retval;
}