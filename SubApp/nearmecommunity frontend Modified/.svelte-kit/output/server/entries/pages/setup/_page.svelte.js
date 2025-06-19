import { c as create_ssr_component, b as subscribe, d as add_attribute, v as validate_component } from "../../../chunks/ssr.js";
import { u as user } from "../../../chunks/userStore.js";
import "../../../chunks/client.js";
import "../../../chunks/Toaster.svelte_svelte_type_style_lang.js";
import { L as LoadingSpinner } from "../../../chunks/LoadingSpinner.js";
const Page = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  let $$unsubscribe_user;
  $$unsubscribe_user = subscribe(user, (value) => value);
  let mapContainer;
  let locationName = "";
  $$unsubscribe_user();
  return `<div class="min-h-screen bg-gray-50 dark:bg-gray-900 p-4"><div class="max-w-4xl mx-auto"><div class="bg-white dark:bg-gray-800 rounded-xl shadow-lg overflow-hidden"><div class="p-6"><h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-4" data-svelte-h="svelte-161houy">Set Your Location</h1> <p class="text-gray-600 dark:text-gray-300 mb-6" data-svelte-h="svelte-tb0lia">NearMe is a location-based community app. Please select your location on the map below.
          All content will be restricted to a 15km radius around your location.</p> <div class="w-full h-96 rounded-lg overflow-hidden mb-6 bg-gray-100 dark:bg-gray-700"${add_attribute("this", mapContainer, 0)}>${`<div class="h-full flex flex-col items-center justify-center">${validate_component(LoadingSpinner, "LoadingSpinner").$$render($$result, { size: "lg" }, {}, {})} <p class="mt-4 text-gray-600 dark:text-gray-300" data-svelte-h="svelte-1nif57m">Loading map...</p></div>`}</div> <div class="mb-6"><label for="locationName" class="label" data-svelte-h="svelte-tb3t2y">Location Name (Optional)</label> <input id="locationName" type="text" placeholder="Home, Work, etc." class="input"${add_attribute("value", locationName, 0)}> <p class="mt-1 text-sm text-gray-500 dark:text-gray-400" data-svelte-h="svelte-2965ri">Give your location a name for easier identification</p></div> <div class="flex space-x-4"><button class="btn btn-primary flex items-center justify-center" ${"disabled"}>${`Save Location`}</button> <button class="btn btn-outline" ${""}>Cancel</button></div></div> <div class="bg-gray-50 dark:bg-gray-700 px-6 py-4" data-svelte-h="svelte-1e2ai3e"><div class="flex items-center"><div class="flex-shrink-0 text-warning-500"><svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path></svg></div> <div class="ml-3"><p class="text-sm text-gray-700 dark:text-gray-300">Your location will be used to show relevant community content within a 15km radius.
              You&#39;ll only need to set this once.</p></div></div></div></div></div></div>`;
});
export {
  Page as default
};
