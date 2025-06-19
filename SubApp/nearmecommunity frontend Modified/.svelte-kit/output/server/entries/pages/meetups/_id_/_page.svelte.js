import { c as create_ssr_component, b as subscribe, v as validate_component } from "../../../../chunks/ssr.js";
import { p as page } from "../../../../chunks/stores.js";
import { u as user } from "../../../../chunks/userStore.js";
import "../../../../chunks/client.js";
import { L as LoadingSpinner } from "../../../../chunks/LoadingSpinner.js";
import "../../../../chunks/Toaster.svelte_svelte_type_style_lang.js";
const Page = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  let $$unsubscribe_page;
  let $$unsubscribe_user;
  $$unsubscribe_page = subscribe(page, (value) => value);
  $$unsubscribe_user = subscribe(user, (value) => value);
  $$unsubscribe_page();
  $$unsubscribe_user();
  return `<div class="min-h-screen bg-gray-50 dark:bg-gray-900 pt-16 pb-20"><div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">${`<div class="flex justify-center py-20">${validate_component(LoadingSpinner, "LoadingSpinner").$$render($$result, { size: "lg" }, {}, {})}</div>`}</div></div>`;
});
export {
  Page as default
};
