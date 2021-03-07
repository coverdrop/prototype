#include "CoverDrop_SGX_ToUsers_u.h"
#include <errno.h>

typedef struct ms_initialize_t {
	sgx_status_t ms_retval;
	size_t ms_msglength;
} ms_initialize_t;

typedef struct ms_process_message_t {
	sgx_status_t ms_retval;
	char* ms_toEnclave;
	size_t ms_len;
	uint8_t* ms_processedNum;
} ms_process_message_t;

typedef struct ms_clean_array_t {
	sgx_status_t ms_retval;
} ms_clean_array_t;

typedef struct ms_ocall_print_string_t {
	const char* ms_str;
} ms_ocall_print_string_t;

typedef struct ms_transmit_message_to_users_t {
	int ms_retval;
	const char* ms_str;
	size_t ms_len;
} ms_transmit_message_to_users_t;

typedef struct ms_sgx_oc_cpuidex_t {
	int* ms_cpuinfo;
	int ms_leaf;
	int ms_subleaf;
} ms_sgx_oc_cpuidex_t;

typedef struct ms_sgx_thread_wait_untrusted_event_ocall_t {
	int ms_retval;
	const void* ms_self;
} ms_sgx_thread_wait_untrusted_event_ocall_t;

typedef struct ms_sgx_thread_set_untrusted_event_ocall_t {
	int ms_retval;
	const void* ms_waiter;
} ms_sgx_thread_set_untrusted_event_ocall_t;

typedef struct ms_sgx_thread_setwait_untrusted_events_ocall_t {
	int ms_retval;
	const void* ms_waiter;
	const void* ms_self;
} ms_sgx_thread_setwait_untrusted_events_ocall_t;

typedef struct ms_sgx_thread_set_multiple_untrusted_events_ocall_t {
	int ms_retval;
	const void** ms_waiters;
	size_t ms_total;
} ms_sgx_thread_set_multiple_untrusted_events_ocall_t;

static sgx_status_t SGX_CDECL CoverDrop_SGX_ToUsers_ocall_print_string(void* pms)
{
	ms_ocall_print_string_t* ms = SGX_CAST(ms_ocall_print_string_t*, pms);
	ocall_print_string(ms->ms_str);

	return SGX_SUCCESS;
}

static sgx_status_t SGX_CDECL CoverDrop_SGX_ToUsers_transmit_message_to_users(void* pms)
{
	ms_transmit_message_to_users_t* ms = SGX_CAST(ms_transmit_message_to_users_t*, pms);
	ms->ms_retval = transmit_message_to_users(ms->ms_str, ms->ms_len);

	return SGX_SUCCESS;
}

static sgx_status_t SGX_CDECL CoverDrop_SGX_ToUsers_sgx_oc_cpuidex(void* pms)
{
	ms_sgx_oc_cpuidex_t* ms = SGX_CAST(ms_sgx_oc_cpuidex_t*, pms);
	sgx_oc_cpuidex(ms->ms_cpuinfo, ms->ms_leaf, ms->ms_subleaf);

	return SGX_SUCCESS;
}

static sgx_status_t SGX_CDECL CoverDrop_SGX_ToUsers_sgx_thread_wait_untrusted_event_ocall(void* pms)
{
	ms_sgx_thread_wait_untrusted_event_ocall_t* ms = SGX_CAST(ms_sgx_thread_wait_untrusted_event_ocall_t*, pms);
	ms->ms_retval = sgx_thread_wait_untrusted_event_ocall(ms->ms_self);

	return SGX_SUCCESS;
}

static sgx_status_t SGX_CDECL CoverDrop_SGX_ToUsers_sgx_thread_set_untrusted_event_ocall(void* pms)
{
	ms_sgx_thread_set_untrusted_event_ocall_t* ms = SGX_CAST(ms_sgx_thread_set_untrusted_event_ocall_t*, pms);
	ms->ms_retval = sgx_thread_set_untrusted_event_ocall(ms->ms_waiter);

	return SGX_SUCCESS;
}

static sgx_status_t SGX_CDECL CoverDrop_SGX_ToUsers_sgx_thread_setwait_untrusted_events_ocall(void* pms)
{
	ms_sgx_thread_setwait_untrusted_events_ocall_t* ms = SGX_CAST(ms_sgx_thread_setwait_untrusted_events_ocall_t*, pms);
	ms->ms_retval = sgx_thread_setwait_untrusted_events_ocall(ms->ms_waiter, ms->ms_self);

	return SGX_SUCCESS;
}

static sgx_status_t SGX_CDECL CoverDrop_SGX_ToUsers_sgx_thread_set_multiple_untrusted_events_ocall(void* pms)
{
	ms_sgx_thread_set_multiple_untrusted_events_ocall_t* ms = SGX_CAST(ms_sgx_thread_set_multiple_untrusted_events_ocall_t*, pms);
	ms->ms_retval = sgx_thread_set_multiple_untrusted_events_ocall(ms->ms_waiters, ms->ms_total);

	return SGX_SUCCESS;
}

static const struct {
	size_t nr_ocall;
	void * func_addr[7];
} ocall_table_CoverDrop_SGX_ToUsers = {
	7,
	{
		(void*)(uintptr_t)CoverDrop_SGX_ToUsers_ocall_print_string,
		(void*)(uintptr_t)CoverDrop_SGX_ToUsers_transmit_message_to_users,
		(void*)(uintptr_t)CoverDrop_SGX_ToUsers_sgx_oc_cpuidex,
		(void*)(uintptr_t)CoverDrop_SGX_ToUsers_sgx_thread_wait_untrusted_event_ocall,
		(void*)(uintptr_t)CoverDrop_SGX_ToUsers_sgx_thread_set_untrusted_event_ocall,
		(void*)(uintptr_t)CoverDrop_SGX_ToUsers_sgx_thread_setwait_untrusted_events_ocall,
		(void*)(uintptr_t)CoverDrop_SGX_ToUsers_sgx_thread_set_multiple_untrusted_events_ocall,
	}
};

sgx_status_t initialize(sgx_enclave_id_t eid, sgx_status_t* retval, size_t msglength)
{
	sgx_status_t status;
	ms_initialize_t ms;
	ms.ms_msglength = msglength;
	status = sgx_ecall(eid, 0, &ocall_table_CoverDrop_SGX_ToUsers, &ms);
	if (status == SGX_SUCCESS && retval) *retval = ms.ms_retval;
	return status;
}

sgx_status_t process_message(sgx_enclave_id_t eid, sgx_status_t* retval, char* toEnclave, size_t len, uint8_t* processedNum)
{
	sgx_status_t status;
	ms_process_message_t ms;
	ms.ms_toEnclave = toEnclave;
	ms.ms_len = len;
	ms.ms_processedNum = processedNum;
	status = sgx_ecall(eid, 1, &ocall_table_CoverDrop_SGX_ToUsers, &ms);
	if (status == SGX_SUCCESS && retval) *retval = ms.ms_retval;
	return status;
}

sgx_status_t clean_array(sgx_enclave_id_t eid, sgx_status_t* retval)
{
	sgx_status_t status;
	ms_clean_array_t ms;
	status = sgx_ecall(eid, 2, &ocall_table_CoverDrop_SGX_ToUsers, &ms);
	if (status == SGX_SUCCESS && retval) *retval = ms.ms_retval;
	return status;
}

