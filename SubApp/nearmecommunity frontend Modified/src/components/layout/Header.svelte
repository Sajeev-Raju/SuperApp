            <div class="relative">
              <button
                class="flex items-center gap-2 text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white"
                on:click={() => (isUserMenuOpen = !isUserMenuOpen)}
              >
                <div class="h-8 w-8 rounded-full bg-purple-100 dark:bg-purple-900/40 flex items-center justify-center">
                  <span class="text-sm font-medium text-purple-600 dark:text-purple-400">
                    {$user.userId.charAt(0).toUpperCase()}
                  </span>
                </div>
                <span class="text-sm font-medium">{$user.userId}</span>
                <svg
                  class="h-5 w-5 {isUserMenuOpen ? 'rotate-180' : ''} transition-transform"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                >
                  <path
                    fill-rule="evenodd"
                    d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                    clip-rule="evenodd"
                  />
                </svg>
              </button>

              {#if isUserMenuOpen}
                <div
                  class="absolute right-0 mt-2 w-48 rounded-md shadow-lg bg-white dark:bg-gray-800 ring-1 ring-black ring-opacity-5 focus:outline-none"
                  role="menu"
                  aria-orientation="vertical"
                  aria-labelledby="user-menu"
                >
                  <div class="py-1" role="none">
                    <a
                      href="/profile"
                      class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                      role="menuitem"
                      on:click={() => (isUserMenuOpen = false)}
                    >
                      Your Profile
                    </a>
                    <button
                      class="block w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                      role="menuitem"
                      on:click={() => {
                        isUserMenuOpen = false;
                        handleSignOut();
                      }}
                    >
                      Sign out
                    </button>
                  </div>
                </div>
              {/if}
            </div> 