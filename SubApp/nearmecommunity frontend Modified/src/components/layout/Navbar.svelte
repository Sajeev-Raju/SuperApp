<script lang="ts">
  import { onMount } from "svelte";
  import { page } from "$app/stores";
  import { user } from "$stores/userStore";
  import { theme } from "$stores/themeStore";
  import { goto } from "$app/navigation";
  import Logo from "$components/ui/Logo.svelte";
  
  let isMobileMenuOpen = false;
  let isUserMenuOpen = false;
  let isScrolled = false;
  
  // Check if the current route matches the given path
  function isActive(path: string): boolean {
    const currentPath = $page.url.pathname;
    // Remove trailing slash if present
    const normalizedPath = path.endsWith('/') ? path.slice(0, -1) : path;
    const normalizedCurrentPath = currentPath.endsWith('/') ? currentPath.slice(0, -1) : currentPath;
    
    return normalizedCurrentPath === normalizedPath;
  }
  
  // Toggle mobile menu
  function toggleMobileMenu() {
    isMobileMenuOpen = !isMobileMenuOpen;
    if (isMobileMenuOpen) {
      isUserMenuOpen = false;
    }
  }
  
  // Toggle user menu
  function toggleUserMenu() {
    isUserMenuOpen = !isUserMenuOpen;
    if (isUserMenuOpen) {
      isMobileMenuOpen = false;
    }
  }
  
  // Logout function
  function logout() {
    user.clearUser();
    goto("/");
  }
  
  // Handle scroll events to change navbar appearance
  onMount(() => {
    const handleScroll = () => {
      isScrolled = window.scrollY > 10;
    };
    
    window.addEventListener("scroll", handleScroll);
    
    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  });
  
  // Navigation items
  const navItems = [
    { name: "Dashboard", path: "/dashboard", icon: "home" },
    { name: "Q&A", path: "/questions", icon: "question-mark" },
    { name: "Meetups", path: "/meetups", icon: "calendar" },
    { name: "Classifieds", path: "/classifieds", icon: "tag" },
    { name: "Polls", path: "/polls", icon: "chart" },
    { name: "Business", path: "/business", icon: "store" },
    { name: "Emergency", path: "/emergency", icon: "alert" }
  ];
</script>

<nav class={`fixed top-0 left-0 right-0 z-50 transition-all duration-200 border-b border-gray-200 dark:border-gray-700 ${isScrolled ? 'bg-white dark:bg-black shadow-md' : 'bg-white/80 dark:bg-black/80 backdrop-blur-sm'}`}>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <div class="flex justify-between h-16">
      <div class="flex">
        <div class="flex-shrink-0 flex items-center">
          <a href="/dashboard" class="flex items-center">
            <Logo />
            <span class="ml-2 text-xl font-bold text-gray-900 dark:text-white">NearMe</span>
          </a>
        </div>
        
        <div class="hidden sm:ml-6 sm:flex sm:space-x-4">
          {#each navItems as item}
            <a 
              href={item.path}
              class={`inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium ${isActive(item.path) 
                ? 'border-purple-500 text-gray-900 dark:text-white'
                : 'border-transparent text-gray-500 dark:text-gray-300 hover:text-gray-700 dark:hover:text-white hover:border-purple-300 dark:hover:border-purple-400'}`}
            >
              {item.name}
            </a>
          {/each}
        </div>
      </div>
      
      <div class="hidden sm:ml-6 sm:flex sm:items-center space-x-4">
        <button
          type="button"
          class="p-2 rounded-lg text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
          on:click={() => theme.toggleTheme()}
        >
          <span class="sr-only">Toggle theme</span>
          {#if $theme === 'dark'}
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          {:else}
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
            </svg>
          {/if}
        </button>

        <button
          type="button"
          class="p-1 rounded-full text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
        >
          <span class="sr-only">View notifications</span>
          <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
        </button>
        
        <div class="ml-3 relative">
          <div>
            <button
              type="button"
              class="flex text-sm rounded-full focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              on:click={toggleUserMenu}
            >
              <span class="sr-only">Open user menu</span>
              <div class="h-8 w-8 rounded-full bg-primary-500 flex items-center justify-center text-white">
                {$user.userId ? $user.userId.charAt(0).toUpperCase() : "U"}
              </div>
            </button>
          </div>
          
          {#if isUserMenuOpen}
            <div
              class="origin-top-right absolute right-0 mt-2 w-48 rounded-md shadow-lg bg-white dark:bg-gray-700 ring-1 ring-black ring-opacity-5 focus:outline-none"
              role="menu"
            >
              <div class="py-1" role="none">
                <div class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-200">
                  User ID: {$user.userId}
                </div>
                <a
                  href="/profile"
                  class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-600"
                  role="menuitem"
                >
                  Your Profile
                </a>
                
                <button
                  on:click={logout}
                  class="w-full text-left block px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-600"
                  role="menuitem"
                >
                  Sign out
                </button>
              </div>
            </div>
          {/if}
        </div>
      </div>
      
      <div class="flex items-center sm:hidden">
        <button
          type="button"
          class="inline-flex items-center justify-center p-2 rounded-md text-gray-400 hover:text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-primary-500"
          on:click={toggleMobileMenu}
        >
          <span class="sr-only">Open main menu</span>
          {#if isMobileMenuOpen}
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          {:else}
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          {/if}
        </button>
      </div>
    </div>
  </div>
  
  {#if isMobileMenuOpen}
    <div class="sm:hidden bg-white dark:bg-black border-t border-gray-200 dark:border-gray-700">
      <div class="pt-2 pb-3 space-y-1">
        {#each navItems as item}
          <a
            href={item.path}
            class={`block pl-3 pr-4 py-2 border-l-4 text-base font-medium ${isActive(item.path)
              ? 'border-primary-500 text-primary-700 dark:text-primary-300 bg-primary-50 dark:bg-primary-900/20'
              : 'border-transparent text-gray-500 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'}`}
          >
            {item.name}
          </a>
        {/each}
      </div>
      
      <div class="pt-4 pb-3 border-t border-gray-200 dark:border-gray-700">
        <div class="flex items-center px-4">
          <div class="flex-shrink-0">
            <div class="h-10 w-10 rounded-full bg-primary-500 flex items-center justify-center text-white">
              {$user.userId ? $user.userId.charAt(0).toUpperCase() : "U"}
            </div>
          </div>
          <div class="ml-3">
            <div class="text-base font-medium text-gray-800 dark:text-white">User</div>
            <div class="text-sm font-medium text-gray-500 dark:text-gray-400">ID: {$user.userId}</div>
          </div>
          <button
            type="button"
            class="ml-auto p-2 rounded-lg text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
            on:click={() => theme.toggleTheme()}
          >
            <span class="sr-only">Toggle theme</span>
            {#if $theme === 'dark'}
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            {:else}
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
              </svg>
            {/if}
          </button>
        </div>
        <div class="mt-3 space-y-1">
          <a
            href="/profile"
            class="block px-4 py-2 text-base font-medium text-gray-500 dark:text-gray-300 hover:text-gray-800 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            Your Profile
          </a>
          <a
            href="/settings"
            class="block px-4 py-2 text-base font-medium text-gray-500 dark:text-gray-300 hover:text-gray-800 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            Settings
          </a>
          <button
            on:click={logout}
            class="w-full text-left block px-4 py-2 text-base font-medium text-gray-500 dark:text-gray-300 hover:text-gray-800 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            Sign out
          </button>
        </div>
      </div>
    </div>
  {/if}
</nav>