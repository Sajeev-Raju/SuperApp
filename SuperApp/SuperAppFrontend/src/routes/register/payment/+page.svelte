<script lang="ts">
  // import { state } from 'svelte';
  import { writable } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { Circle } from 'svelte-loading-spinners';
  import { onMount, onDestroy } from 'svelte';
  import { api } from '$lib/api';
  import QRCode from 'qrcode';

  const formState = $state({
    loading: true,
    error: null as string | null,
    paymentData: null as any,
    qrImageUrl: null as string | null,
    upiPaymentLink: null as string | null,
    paymentLinkId: null as string | null,
    orderId: null as string | null,
    paymentStatus: 'PENDING' as 'PENDING' | 'SUCCESS' | 'FAILED',
    pollingInterval: null as any
  });

  onMount(() => {
    const data = sessionStorage.getItem('paymentData');
    if (!data) {
      goto('/register');
      return;
    }

    formState.paymentData = JSON.parse(data);
    initiateQRPayment();
  });

  async function initiateQRPayment() {
    try {
      console.log('Original payment data:', formState.paymentData);

      // Validate phone number format
      const phoneRegex = /^\+?[0-9]{10,15}$/;
      if (!phoneRegex.test(formState.paymentData.phone)) {
        formState.error = 'Invalid phone number format. Must be 10-15 digits with optional + prefix.';
        formState.loading = false;
        return;
      }

      // Validate email format
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(formState.paymentData.email)) {
        formState.error = 'Invalid email format.';
        formState.loading = false;
        return;
      }

      // Ensure we're sending exactly what the backend expects
      const paymentRequest = {
        email: formState.paymentData.email.trim(),
        phone: formState.paymentData.phone.trim(),
        username: formState.paymentData.username.trim(),
        isFancy: Boolean(formState.paymentData.isFancy),
        fancyType: formState.paymentData.fancyType || null,
        basePrice: Number(formState.paymentData.basePrice),
        fancyPrice: Number(formState.paymentData.fancyPrice || 0),
        totalPrice: Number(formState.paymentData.totalPrice)
      };

      // Validate required numeric fields
      if (isNaN(paymentRequest.basePrice) || isNaN(paymentRequest.totalPrice)) {
        formState.error = 'Invalid price values.';
        formState.loading = false;
        return;
      }

      console.log('Sending payment request:', JSON.stringify(paymentRequest, null, 2));

      const response = await api.initiateQRPayment(paymentRequest);

      if (response.data.success) {
        formState.upiPaymentLink = response.data.data.upiPaymentLink;
        formState.paymentLinkId = response.data.data.paymentLinkId;
        
        // Open UPI payment link in new tab
        window.open(formState.upiPaymentLink, '_blank');
        
        formState.loading = false;
        startPolling();
      } else {
        formState.error = response.data.message;
        formState.loading = false;
      }
    } catch (error: any) {
      console.error('Payment initiation error:', error);
      console.error('Error response:', error.response?.data);
      console.error('Error status:', error.response?.status);
      console.error('Error headers:', error.response?.headers);
      formState.error = error.response?.data?.message || error.message;
      formState.loading = false;
    }
  }

  function startPolling() {
    formState.pollingInterval = setInterval(checkPaymentStatus, 5000); // Poll every 5 seconds
  }

  async function checkPaymentStatus() {
    if (!formState.paymentLinkId) return;
    
    try {
      const response = await api.checkPaymentStatus(formState.paymentLinkId);
      
      if (response.data.success) {
        if (response.data.data.status === 'COMPLETED') {
          formState.paymentStatus = 'SUCCESS';
          clearInterval(formState.pollingInterval);
          await completeRegistration();
        } else if (response.data.data.status === 'FAILED') {
          formState.paymentStatus = 'FAILED';
          formState.error = 'Payment failed. Please try again.';
          clearInterval(formState.pollingInterval);
        }
      }
    } catch (error: any) {
      console.error('Error checking payment status:', error);
      // Continue polling despite errors
    }
  }

  async function completeRegistration() {
    try {
      const response = await api.completeRegistration({
        email: formState.paymentData.email,
        phone: formState.paymentData.phone,
        username: formState.paymentData.username
      });

      if (response.data.success) {
        sessionStorage.removeItem('registration');
        sessionStorage.removeItem('paymentData');
        goto('/register/success');
      } else {
        formState.error = 'Failed to complete registration';
      }
    } catch (error: any) {
      formState.error = error.response?.data?.message || error.message;
    }
  }

  onDestroy(() => {
    if (formState.pollingInterval) {
      clearInterval(formState.pollingInterval);
    }
  });
</script>

<div class="max-w-md mx-auto">
  <div class="card">
    <h1 class="text-2xl font-bold mb-6">Complete Payment</h1>

    {#if formState.loading}
      <div class="flex flex-col items-center justify-center py-8 space-y-4">
        <Circle size="40" color="#0ea5e9" unit="px" duration="1s" />
        <p>Initializing payment...</p>
      </div>
    {:else if formState.error}
      <div class="mt-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative">
        <strong class="font-bold">Payment Failed!</strong>
        <span class="block sm:inline">{formState.error}</span>
        <button 
          class="btn btn-secondary mt-4 w-full"
          on:click={() => window.location.reload()}
        >
          Try Again
        </button>
      </div>
    {:else}
      <div class="text-center space-y-6">
        <div class="p-6 rounded-lg bg-gray-50 dark:bg-gray-800">
          <h2 class="text-xl font-semibold mb-2">Payment Details</h2>
          <p class="text-gray-600 dark:text-gray-400">Amount: ₹{formState.paymentData.totalPrice}</p>
          <p class="text-gray-600 dark:text-gray-400">Username: {formState.paymentData.username}</p>
        </div>

        <div class="space-y-4">
          <p class="text-sm text-gray-600 dark:text-gray-400">
            A new tab has been opened with the payment link. Please complete the payment there.
          </p>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            If the payment window didn't open automatically, 
            <a href={formState.upiPaymentLink} target="_blank" class="text-primary-600 hover:underline">click here</a> to pay.
          </p>
        </div>

        {#if formState.paymentStatus === 'PENDING'}
          <div class="flex items-center justify-center space-x-2">
            <Circle size="20" color="#0ea5e9" unit="px" duration="1s" />
            <span class="text-sm text-gray-600 dark:text-gray-400">Waiting for payment...</span>
          </div>
        {/if}
      </div>
    {/if}
  </div>
</div>