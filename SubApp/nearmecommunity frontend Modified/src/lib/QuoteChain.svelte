<script lang="ts">
  export let quoteChain: { id: string; excerpt: string }[] = [];
  export let body: string = "";
</script>

{#if quoteChain.length > 1}
  <div class="quote-box">
    <svelte:self quoteChain={quoteChain.slice(0, -1)} body={quoteChain[quoteChain.length - 1].excerpt} />
  </div>
{:else if quoteChain.length === 1}
  <div class="quote-box">
    {quoteChain[0].excerpt}
  </div>
{/if}
{#if body}
  <div class="reply-body">{body}</div>
{/if}

<style>
  .quote-box {
    border-left: 3px solid #ccc;
    margin-left: 1rem;
    padding-left: 1rem;
    margin-bottom: 0.5rem;
    background: #f9f9f9;
    font-style: italic;
  }
  .reply-body {
    margin-bottom: 1.5rem;
    padding: 0.5rem 0;
  }
</style> 