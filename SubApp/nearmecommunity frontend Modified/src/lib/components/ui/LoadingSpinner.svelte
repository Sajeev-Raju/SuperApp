<script lang="ts">
  export let size: 'sm' | 'md' | 'lg' = 'md';
  export let className: string = '';

  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-8 h-8',
    lg: 'w-12 h-12'
  };
</script>

<div class="flex justify-center items-center">
  <div class={`animate-spin rounded-full border-4 border-gray-200 border-t-primary-600 ${sizeClasses[size]} ${className}`}></div>
</div> 