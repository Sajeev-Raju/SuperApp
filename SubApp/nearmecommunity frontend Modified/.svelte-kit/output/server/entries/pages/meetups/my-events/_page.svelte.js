import { c as create_ssr_component, b as subscribe, v as validate_component } from "../../../../chunks/ssr.js";
import { u as user } from "../../../../chunks/userStore.js";
import "../../../../chunks/client.js";
import { L as LoadingSpinner } from "../../../../chunks/LoadingSpinner.js";
import "../../../../chunks/Toaster.svelte_svelte_type_style_lang.js";
const Page = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  let $$unsubscribe_user;
  $$unsubscribe_user = subscribe(user, (value) => value);
  $$unsubscribe_user();
  return `<div class="min-h-screen bg-gray-50 dark:bg-gray-900 pt-16 pb-20"><div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8"><div class="py-6"><div class="flex flex-col md:flex-row md:items-center md:justify-between mb-8" data-svelte-h="svelte-1io4blv"><div><h1 class="text-3xl font-bold text-gray-900 dark:text-white">My Events</h1> <p class="mt-2 text-gray-600 dark:text-gray-400">Manage your organized meetups</p></div> <div class="mt-4 md:mt-0"><a href="/meetups/create" class="btn btn-primary">Create New Meetup</a></div></div> ${`<div class="flex justify-center py-20">${validate_component(LoadingSpinner, "LoadingSpinner").$$render($$result, { size: "lg" }, {}, {})}</div>`}</div></div></div>`;
});
export {
  Page as default
};
