<script lang="ts">
  import { user } from '$stores/userStore';
  export let replyTree = [];
  export let question;
  export let replyStates;
  export let handleReplySubmit;
  export let handleDeleteAnswer;
</script>

<ul class="nested-list">
  {#each replyTree as node (node.id)}
    {#if node && node.body !== undefined}
      <li class="nested-item">
        <div class="bg-gray-50 dark:bg-black/70 rounded-lg p-4 border border-gray-300 dark:border-gray-700">
          <div class="font-semibold text-gray-800 dark:text-gray-100 mb-1">
            {node.body}
          </div>
          <div class="mt-2 flex items-center gap-2">
            <button class="btn btn-secondary" on:click={() => {
              replyStates[node.id] = { open: true, content: "" };
            }}>Reply</button>
            {#if node.userId === $user.userId}
              <button class="btn btn-danger btn-xs ml-2" on:click={() => handleDeleteAnswer(node.id)}>
                Delete
              </button>
            {/if}
          </div>
          {#if replyStates[node.id]?.open}
            <textarea
              bind:value={replyStates[node.id].content}
              rows="2"
              placeholder="Write your reply..."
              class="input w-full mb-2"
            ></textarea>
            <button class="btn btn-primary mr-2" on:click={() => handleReplySubmit(node.answer)}>Reply</button>
            <button class="btn btn-outline" on:click={() => replyStates[node.id].open = false}>Cancel</button>
          {/if}
        </div>
        {#if node.children && Array.isArray(node.children) && node.children.length > 0}
          <div class="ml-6 mt-2">
            <SafeNestedReplyTree replyTree={node.children} {question} {replyStates} {handleReplySubmit} {handleDeleteAnswer} />
          </div>
        {/if}
      </li>
    {/if}
  {/each}
</ul>

<style>
.nested-list {
  list-style: none;
  padding-left: 0;
}
.nested-item {
  margin-bottom: 1rem;
}
</style> 