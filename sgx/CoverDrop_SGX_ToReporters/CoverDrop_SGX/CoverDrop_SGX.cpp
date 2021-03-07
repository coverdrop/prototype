#include "CoverDrop_SGX_t.h"
#include "CoverDrop_SGX.h"
#include <string>
#include <cstdlib>
#include <stdio.h>
#include <random>
#include <sgx_trts.h>

#define REPORTERNUM 9 //For now, we only allow reporters 1-9
#define REPORTERBUFSIZE 50 //We can keep up to 20 messages per reporter in buffer
//The number of messages (dummy+legit) that need to be processed before a transmission occurs. 
//This is set to be smaller than REPORTERBUFSIZE to avoid correlations.
//TODO: Think about increasing TRIGGERNUM. The larger this number the lower the bandwidth consumed
#define TRIGGERNUM 1 

const size_t MAXMESSAGESIZE = 200;
const size_t MINMESSAGESIZE = 100;
const char legitIndicator = 'L';

std::string ReporterBuf[REPORTERNUM][REPORTERBUFSIZE]; //Cache for reporters
int pendingMessages[REPORTERNUM]; //Keeping track of pending messages per reporter
int currentlyProcessed; //Number of messages processed since last transmission

size_t message_length; //Length of all messages from users to reporters;

std::string create_dummy(std::string toSend, size_t len, bool shouldAssign);
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

sgx_status_t clean_arrays() {
	std::string reset = "";
	for (int i = 0; i < REPORTERNUM; i++) {
		pendingMessages[i] = 0;
		for(int j = 0; j < REPORTERBUFSIZE; j++)
			ReporterBuf[i][j] = reset;
	}
	currentlyProcessed = 0;
	printf("Successfully cleaned arrays");
	return SGX_SUCCESS;
}

/*
* process_message:
*   Invoked by ECALL to take in a single message and process it
*	This caches legit messages and triggers transmission if currentlyProcessed reaches a threshold
*	The untrusted app sends a DELETE for the messages that was sent here.
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

	if (toEnclave[0] != legitIndicator) {
		//This is a dummy message
		//We could return from this point but that would lead to timing attacks.
		//The rest of this of body is useless except to make such attacks more difficult.
		temp = (char)(toEnclave[1] - 0);
		tempstring = charray_to_string(toEnclave, msglength);
		temp++;
	}
	else {
		//This is a legitimate message
		int RID = (char) (toEnclave[1] - '0');
		ReporterBuf[RID][pendingMessages[RID]] = charray_to_string(toEnclave, msglength);
		pendingMessages[RID]++;
	}

	//If you have processed enough messages then request transmission
	if (currentlyProcessed == TRIGGERNUM) {
		int retval;
		printf("\nTriggering transmission\n");
		for (int i = 0; i < REPORTERNUM; i++) //0th reporter doesn't exist
		{
			for (int j = 0; j < REPORTERBUFSIZE; j++) {
				std::string temp = ReporterBuf[i][j];
				if (pendingMessages[i] <= j)
					ReporterBuf[i][j] = create_dummy(temp, msglength, true);
				else
					ReporterBuf[i][j] = create_dummy(temp, msglength, false);
				temp = ReporterBuf[i][j];
				temp[msglength - 1] = 0;
				char *cstr = &temp[0];

				transmit_message_to_reporter(&retval, cstr, msglength, (uint8_t)i+1);
				if (retval != 0) {
					//TODO: Improve dealing with failures
					printf("\nERROR SENDING MESSAGE. THIS MESSAGE IS NOW LOST.");
				}

			}
			
		}
		ret = clean_arrays();
		if (ret != SGX_SUCCESS)
			printf("\nFAILED TO CLEAR ARRAYS AFTER TRANSMISSION\n");
	}
	processedNum[0] = (uint8_t)currentlyProcessed;
	printf(toEnclave);
	return ret;
}

//If shouldAssign is true then the dummy message is put in the ReporterBuf array otherwise it is assigned to a thrown-away string
//This is a first-effort to mitigate timing attacks, might need to do more.
std::string create_dummy(std::string toSend, size_t len, bool shouldAssign) {
	std::string toret = "";
	std::string temp = "";
	char receiver[MAXMESSAGESIZE];
	sgx_status_t ret = sgx_read_rand(reinterpret_cast<unsigned char *>(receiver), len);
	receiver[0] = 'D';
	for (int i = 1; i < len; i++) {
		char temp = receiver[i];
		//Deal with unsigned char conversion
		if (temp < 0)
			temp = (char)(-1 * temp);
		temp = (char)(temp  % 26);
		receiver[i] = ( char)(temp + 'a');
	}
	receiver[len - 1] = 0;
	temp = charray_to_string(receiver, len);
	if (shouldAssign) {
		toret = temp;
		/*char *printer = &toret[0];
		printf("\nDummy!\n");
		printf(printer);
		printf("\n"); */
	}
	else {
		toret = toSend;
		/*char *printer = &toret[0];
		printf("\nLegit!\n");
		printf(printer);
		printf("\n");*/
	}

	return toret;
}

std::string charray_to_string(char* in, size_t len) {
	std::string toret = "";
	for (int i = 0; i < len; i++) {
		toret = toret + in[i];
	}
	return toret;
}