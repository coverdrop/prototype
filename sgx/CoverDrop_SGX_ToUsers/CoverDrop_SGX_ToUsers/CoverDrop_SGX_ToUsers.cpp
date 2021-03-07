#include "CoverDrop_SGX_ToUsers_t.h"
#include "CoverDrop_SGX_ToUsers.h"
#include "sgx_trts.h"
#include <string>
#include <cstdlib>
#include <stdio.h>
#include <random>

//TODO, SECURITY: Think about the tradeoff between security and performance in caching
//Sending all dummy messages on preserves perfect mixing but is not performant at ll
//Only sending legit messages may mean that an adversary can do correlation on reporter frequency of sending legit messages?
//Also think about memory access monitoring since only legit messages are assigned to the messagebuf array.

//Here, the enclave simply drops all dummy messages and forwards on all the legit ones to deaddrop


#define TRIGGERNUM 40 

const size_t MAXMESSAGESIZE = 200;
const size_t MINMESSAGESIZE = 100;
const char dummyIndicator = '0'; //Reporter ID 0 is not valid

std::string MessageBuf[TRIGGERNUM]; //Cache for legit messages
int currentlyProcessed; ////Number of legit and dummy messages processed since last transmission
int legitProcessed; //Number of legit messages processed since last transmission

size_t message_length; //Length of all messages from users to reporters;

//std::string create_dummy(std::string toSend, size_t len, bool shouldAssign); //Might need this later if we decide to also send dummies
std::string charray_to_string(char* in, size_t len);
/*
* printf:
*   Invokes OCALL to display the enclave buffer to the terminal.
*/
void printf(const char *fmt, ...)
{
	char buf[BUFSIZ] = { '\0' };
	va_list ap;
	va_start(ap, fmt);
	vsnprintf(buf, BUFSIZ, fmt, ap);
	va_end(ap);
	ocall_print_string(buf);
}


sgx_status_t initialize(size_t msglength)
{
	if (msglength > MAXMESSAGESIZE) {
		printf("Message length too long");
		return SGX_ERROR_INVALID_PARAMETER;
	}
	if (msglength < MINMESSAGESIZE) {
		printf("Message length too long");
		return SGX_ERROR_INVALID_PARAMETER;
	}
	message_length = msglength;
	printf("Initialized. Message length set.\n");
}

sgx_status_t clean_array() {
	std::string reset = "";
	for (int i = 0; i < TRIGGERNUM; i++) {
		MessageBuf[i] = reset;
	}
	currentlyProcessed = 0;
	legitProcessed = 0;
	printf("Successfully cleaned array");
	return SGX_SUCCESS;
}

/*
* process_message:
*   Invoked by ECALL to take in a single message and process it
*	This caches legit messages and triggers transmission if currentlyProcessed reaches a threshold
*	The untrusted app sends a DELETE for the message that was sent here.
*	WARNING: Loss of messages will occur if the enclave gets shut down or if transmission triggered here fails.
*	SECURITY: Potential for timing attack to determine dummy or not. Further investigation needed.
*/
sgx_status_t process_message(char* toEnclave, size_t msglength, uint8_t* processedNum)
{
	sgx_status_t ret = SGX_SUCCESS;
	int temp = 0;
	std::string tempstring = "";
	if (msglength != message_length) {
		printf("\nMessage length not equal to initialized value!\n");
		processedNum[0] = (uint8_t)message_length;
		return SGX_ERROR_INVALID_PARAMETER;
	}
	if (currentlyProcessed >= TRIGGERNUM) {
		printf("Cache full!");
		return SGX_ERROR_UNEXPECTED;
	}

	currentlyProcessed++; //Should this be incremented before the length checks? Unclear.

	if (toEnclave[0] == dummyIndicator) {
		//This is a dummy message
		//We could return from this point but that would lead to timing attacks.
		//The rest of this of body is useless except to make such attacks more difficult.
		tempstring = charray_to_string(toEnclave, msglength);
		temp++;
	}
	else {
		//This is a legitimate message
		MessageBuf[legitProcessed] = charray_to_string(toEnclave, msglength);
		legitProcessed++;
	}

	//If you have processed enough messages then request transmission
	if (currentlyProcessed == TRIGGERNUM) {
		int retval;
		printf("\nTriggering transmission\n");
		for (int i = 0; i < legitProcessed; i++)
		{
			std::string tstr = MessageBuf[i];
			tstr[message_length - 1] = '\0'; //Ensuring string is null terminated before transferring out of enclave
			char *cstr = &tstr[0];
			transmit_message_to_users(&retval, cstr, message_length);
			if (retval != 0) {
				//TODO: Improve dealing with failures
				printf("\nERROR SENDING MESSAGE. THIS MESSAGE IS NOW LOST.");
			}
		}
		ret = clean_array();
		if (ret != SGX_SUCCESS)
			printf("\nFAILED TO CLEAR ARRAYS AFTER TRANSMISSION\n");
	}
	processedNum[0] = (uint8_t)currentlyProcessed;
	printf(toEnclave);
	return ret;
}


std::string charray_to_string(char* in, size_t len) {
	std::string toret = "";
	for (int i = 0; i < len; i++) {
		toret = toret + in[i];
	}
	return toret;
}
