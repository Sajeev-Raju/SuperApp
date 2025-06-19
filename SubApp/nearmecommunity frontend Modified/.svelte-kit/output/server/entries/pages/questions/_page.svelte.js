import { c as create_ssr_component, b as subscribe, v as validate_component, d as add_attribute, e as each, f as escape } from "../../../chunks/ssr.js";
import { u as user } from "../../../chunks/userStore.js";
import "../../../chunks/client.js";
import { L as LoadingSpinner } from "../../../chunks/LoadingSpinner.js";
import { U as UserLocationBadge } from "../../../chunks/UserLocationBadge.js";
const Page = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  let $$unsubscribe_user;
  $$unsubscribe_user = subscribe(user, (value) => value);
  let questions = [];
  let searchQuery = "";
  let selectedTags = [];
  let availableTags = [];
  questions.filter((question) => {
    const matchesTags = selectedTags.length === 0 || selectedTags.every((tag) => question.tags.includes(tag));
    return matchesTags;
  });
  $$unsubscribe_user();
  return `<div class="min-h-screen bg-gray-50 dark:bg-gray-900 pt-16 pb-20"><div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8"><div class="py-6"><div class="flex flex-col md:flex-row md:items-center md:justify-between mb-8"><div><h1 class="text-3xl font-bold text-gray-900 dark:text-white" data-svelte-h="svelte-1u2omo9">Questions &amp; Answers</h1> <div class="mt-2">${validate_component(UserLocationBadge, "UserLocationBadge").$$render($$result, {}, {}, {})}</div></div> <div class="mt-4 md:mt-0" data-svelte-h="svelte-16f9kq0"><a href="/questions/create" class="btn btn-primary">Ask a Question</a></div></div> <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md overflow-hidden mb-6"><div class="p-6"><div class="flex flex-col md:flex-row md:items-center gap-4"><div class="flex-1"><div class="relative"><input type="text" placeholder="Search questions..." class="input pl-10"${add_attribute("value", searchQuery, 0)}> <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none" data-svelte-h="svelte-v4qrfe"><svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg></div></div></div> <div><select class="input"><option value="recent" data-svelte-h="svelte-ijeavb">Most Recent</option><option value="popular" data-svelte-h="svelte-184dggr">Most Popular</option><option value="unanswered" data-svelte-h="svelte-yt95ym">Unanswered</option></select></div></div> ${availableTags.length > 0 ? `<div class="mt-4"><div class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2" data-svelte-h="svelte-uh9az2">Filter by tags:</div> <div class="flex flex-wrap gap-2">${each(availableTags, (tag) => {
    return `<button${add_attribute(
      "class",
      `inline-flex items-center px-3 py-1 rounded-full text-sm font-medium transition-colors ${selectedTags.includes(tag) ? "bg-primary-100 text-primary-800 dark:bg-primary-900/40 dark:text-primary-300" : "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"}`,
      0
    )}>${escape(tag)} </button>`;
  })}</div></div>` : ``}</div></div> ${`<div class="flex justify-center py-20">${validate_component(LoadingSpinner, "LoadingSpinner").$$render($$result, { size: "lg" }, {}, {})}</div>`} <div class="mt-8 flex justify-center" data-svelte-h="svelte-17cvdfo"><nav class="inline-flex rounded-md shadow"><a href="#" class="px-4 py-2 rounded-l-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700">Previous</a> <a href="#" class="px-4 py-2 border-t border-b border-gray-300 dark:border-gray-600 bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300">1</a> <a href="#" class="px-4 py-2 border-t border-b border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700">2</a> <a href="#" class="px-4 py-2 rounded-r-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700">Next</a></nav></div></div></div></div>`;
});
export {
  Page as default
};
