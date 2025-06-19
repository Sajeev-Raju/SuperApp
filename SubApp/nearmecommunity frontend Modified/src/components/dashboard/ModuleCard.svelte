<script lang="ts">
  export let title: string;
  export let description: string;
  export let icon: string;
  export let color: string;
  export let link: string;
  export let count: string | undefined = undefined;
  
  const colorMap = {
    primary: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300",
    secondary: "bg-purple-50 text-purple-600 dark:bg-purple-900/20 dark:text-purple-400",
    accent: "bg-purple-50 text-purple-500 dark:bg-purple-900/10 dark:text-purple-500",
    success: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300",
    danger: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300",
    warning: "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-300",
  };
  
  const iconMap = {
    "home": `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />`,
    "question-mark": `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />`,
    "calendar": `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />`,
    "tag": `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />`,
    "chart": `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />`,
    "alert": `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />`,
    "store": `<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />`,
  };
  
  const colorClass = colorMap[color] || colorMap.primary;
  const iconSvg = iconMap[icon] || iconMap["question-mark"];
</script>

<a href={link} class="module-card block p-6 bg-white dark:bg-black rounded-xl shadow-sm border border-purple-100 dark:border-purple-900/50">
  <div class="flex items-center justify-between mb-4">
    <div class="rounded-full p-3 {colorClass}">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        {@html iconSvg}
      </svg>
    </div>
    {#if count}
      <div class="text-sm font-medium text-gray-500 dark:text-gray-400">
        {count}
      </div>
    {/if}
  </div>
  <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-1">{title}</h3>
  <p class="text-sm text-gray-600 dark:text-gray-300">{description}</p>
</a>