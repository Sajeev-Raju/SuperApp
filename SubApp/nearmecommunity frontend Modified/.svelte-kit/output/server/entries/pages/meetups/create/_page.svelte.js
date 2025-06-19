import { c as create_ssr_component, b as subscribe, d as add_attribute, f as escape, e as each } from "../../../../chunks/ssr.js";
import { u as user } from "../../../../chunks/userStore.js";
import "../../../../chunks/client.js";
import "../../../../chunks/Toaster.svelte_svelte_type_style_lang.js";
const Page = create_ssr_component(($$result, $$props, $$bindings, slots) => {
  let $$unsubscribe_user;
  $$unsubscribe_user = subscribe(user, (value) => value);
  let formData = {
    title: "",
    startDate: "",
    startTime: "",
    endDate: "",
    endTime: "",
    eventAddress: "",
    googleLocationURL: "",
    contactInfo: "",
    maxParticipants: "",
    tags: []
  };
  const popularTags = [
    "Community",
    "Outdoors",
    "Technology",
    "Food",
    "Arts",
    "Sports",
    "Education",
    "Business"
  ];
  $$unsubscribe_user();
  return `<div class="min-h-screen bg-gray-50 dark:bg-gray-900 pt-16 pb-20"><div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8"><div class="py-6"><div class="mb-8" data-svelte-h="svelte-672qar"><h1 class="text-3xl font-bold text-gray-900 dark:text-white">Create New Meetup</h1> <p class="mt-2 text-gray-600 dark:text-gray-400">Fill in the details below to create a new meetup</p></div> ${``} <form class="space-y-6"> <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6"><h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4" data-svelte-h="svelte-19yzf9e">Meetup Image</h2> <div class="space-y-4"><div class="flex items-center justify-center w-full"><label for="image-upload" class="flex flex-col items-center justify-center w-full h-64 border-2 border-gray-300 dark:border-gray-600 border-dashed rounded-lg cursor-pointer bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600">${`<div class="flex flex-col items-center justify-center pt-5 pb-6" data-svelte-h="svelte-6p44hs"><svg class="w-8 h-8 mb-4 text-gray-500 dark:text-gray-400" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 16"><path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 13h3a3 3 0 0 0 0-6h-.025A5.56 5.56 0 0 0 16 6.5 5.5 5.5 0 0 0 5.207 5.021C5.137 5.017 5.071 5 5 5a4 4 0 0 0 0 8h2.167M10 15V6m0 0L8 8m2-2 2 2"></path></svg> <p class="mb-2 text-sm text-gray-500 dark:text-gray-400"><span class="font-semibold">Click to upload</span> or drag and drop</p> <p class="text-xs text-gray-500 dark:text-gray-400">PNG, JPG or JPEG (MAX. 800x400px)</p></div>`} <input id="image-upload" type="file" accept="image/*" class="hidden"></label></div></div></div>  <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6"><h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4" data-svelte-h="svelte-sp2ghb">Basic Information</h2> <div class="space-y-4"><div><label for="title" class="block text-sm font-medium text-gray-700 dark:text-gray-300" data-svelte-h="svelte-1jfgqvf">Title</label> <input type="text" id="title" required class="input mt-1 w-full" placeholder="Enter meetup title"${add_attribute("value", formData.title, 0)}></div> <div><label for="description" class="block text-sm font-medium text-gray-700 dark:text-gray-300" data-svelte-h="svelte-t1qyc3">Description</label> <textarea id="description" required rows="4" class="input mt-1 w-full" placeholder="Enter meetup description">${escape("")}</textarea></div> <div class="grid grid-cols-1 md:grid-cols-2 gap-4"><div><label for="startDate" class="block text-sm font-medium text-gray-700 dark:text-gray-300" data-svelte-h="svelte-yx0yz3">Start Date</label> <input type="date" id="startDate" required class="input mt-1 w-full"${add_attribute("value", formData.startDate, 0)}></div> <div><label for="startTime" class="block text-sm font-medium text-gray-700 dark:text-gray-300" data-svelte-h="svelte-ztddf9">Start Time</label> <input type="time" id="startTime" required class="input mt-1 w-full"${add_attribute("value", formData.startTime, 0)}></div></div> <div class="grid grid-cols-1 md:grid-cols-2 gap-4"><div><label for="endDate" class="block text-sm font-medium text-gray-700 dark:text-gray-300" data-svelte-h="svelte-pthr2j">End Date</label> <input type="date" id="endDate" required class="input mt-1 w-full"${add_attribute("value", formData.endDate, 0)}></div> <div><label for="endTime" class="block text-sm font-medium text-gray-700 dark:text-gray-300" data-svelte-h="svelte-cowyht">End Time</label> <input type="time" id="endTime" required class="input mt-1 w-full"${add_attribute("value", formData.endTime, 0)}></div></div></div></div>  <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6"><h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4" data-svelte-h="svelte-10netxg">Location Information</h2> <div class="space-y-4"><div><label for="eventAddress" class="block text-sm font-medium text-gray-700 dark:text-gray-300" data-svelte-h="svelte-n2uj3d">Event Address</label> <input type="text" id="eventAddress" required class="input mt-1 w-full" placeholder="Enter event address"${add_attribute("value", formData.eventAddress, 0)}></div> <div><label for="googleLocationURL" class="block text-sm font-medium text-gray-700 dark:text-gray-300" data-svelte-h="svelte-18y7xo9">Google Maps URL</label> <input type="url" id="googleLocationURL" class="input mt-1 w-full" placeholder="Enter Google Maps URL (optional)"${add_attribute("value", formData.googleLocationURL, 0)}></div></div></div>  <div class="bg-white dark:bg-gray-800 rounded-xl shadow-md p-6"><h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4" data-svelte-h="svelte-1kw3yei">Additional Information</h2> <div class="space-y-4"><div><label for="contactInfo" class="block text-sm font-medium text-gray-700 dark:text-gray-300" data-svelte-h="svelte-f128pp">Contact Information</label> <input type="text" id="contactInfo" class="input mt-1 w-full" placeholder="Enter contact information"${add_attribute("value", formData.contactInfo, 0)}></div> <div><label for="maxParticipants" class="block text-sm font-medium text-gray-700 dark:text-gray-300" data-svelte-h="svelte-11ddcz">Maximum Participants</label> <input type="number" id="maxParticipants" min="1" class="input mt-1 w-full" placeholder="Enter maximum number of participants"${add_attribute("value", formData.maxParticipants, 0)}></div> <div><label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2" data-svelte-h="svelte-1mse7dy">Tags</label> <div class="flex flex-wrap gap-2">${each(popularTags, (tag) => {
    return `<button type="button"${add_attribute(
      "class",
      `inline-flex items-center px-3 py-1 rounded-full text-sm font-medium transition-colors ${formData.tags.includes(tag) ? "bg-secondary-100 text-secondary-800 dark:bg-secondary-900/40 dark:text-secondary-300" : "bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600"}`,
      0
    )}>${escape(tag)} </button>`;
  })}</div></div></div></div> <div class="flex justify-end gap-4"><button type="button" class="btn btn-outline" data-svelte-h="svelte-1hv030p">Cancel</button> <button type="submit" class="btn btn-primary" ${""}>${`Create Meetup`}</button></div></form></div></div></div>`;
});
export {
  Page as default
};
