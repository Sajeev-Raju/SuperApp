import { c as create_ssr_component, b as subscribe, f as escape } from "./ssr.js";
import { u as user } from "./userStore.js";
const UserLocationBadge = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  let $user, $$unsubscribe_user;
  $$unsubscribe_user = subscribe(user, (value) => $user = value);
  $$unsubscribe_user();
  return `${$user.location ? `<div class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-100 text-primary-800 dark:bg-primary-900/40 dark:text-primary-300"><svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd"></path></svg> ${escape($user.location.locationName || "My Location")} <span class="ml-1 text-xs opacity-75" data-svelte-h="svelte-1tae4e8">(15km radius)</span></div>` : ``}`;
});
export {
  UserLocationBadge as U
};
