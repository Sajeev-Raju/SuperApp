import { c as create_ssr_component, b as subscribe, v as validate_component } from "../../../chunks/ssr.js";
import { u as user } from "../../../chunks/userStore.js";
import "../../../chunks/client.js";
import { U as UserLocationBadge } from "../../../chunks/UserLocationBadge.js";
import { L as LoadingSpinner } from "../../../chunks/LoadingSpinner.js";
const Page = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  let $$unsubscribe_user;
  $$unsubscribe_user = subscribe(user, (value) => value);
  $$unsubscribe_user();
  return `<div class="min-h-screen bg-gray-50 dark:bg-gray-900 pt-16 pb-20"><div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8"><div class="py-6"><div class="flex flex-col md:flex-row md:items-center md:justify-between mb-8"><div><h1 class="text-3xl font-bold text-gray-900 dark:text-white" data-svelte-h="svelte-fwn6h5">Dashboard</h1> <div class="mt-2">${validate_component(UserLocationBadge, "UserLocationBadge").$$render($$result, {}, {}, {})}</div></div> <div class="mt-4 md:mt-0" data-svelte-h="svelte-1tkdt9z"><button class="btn btn-primary">New Post</button></div></div> ${`<div class="flex justify-center py-20">${validate_component(LoadingSpinner, "LoadingSpinner").$$render($$result, { size: "lg" }, {}, {})}</div>`}</div></div></div>`;
});
export {
  Page as default
};
