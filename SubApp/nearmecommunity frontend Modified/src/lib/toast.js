// Simple toast notification system
let toastContainer;

function createToastContainer() {
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.className = 'fixed top-4 right-4 z-50';
    document.body.appendChild(toastContainer);
  }
  return toastContainer;
}

function showToast(message, type = 'info') {
  const container = createToastContainer();
  const toast = document.createElement('div');
  
  // Set base classes
  toast.className = `mb-2 p-4 rounded-lg shadow-lg transform transition-all duration-300 translate-x-full`;
  
  // Set type-specific classes
  switch (type) {
    case 'success':
      toast.className += ' bg-green-500 text-white';
      break;
    case 'error':
      toast.className += ' bg-red-500 text-white';
      break;
    case 'warning':
      toast.className += ' bg-yellow-500 text-white';
      break;
    default:
      toast.className += ' bg-blue-500 text-white';
  }
  
  toast.textContent = message;
  container.appendChild(toast);
  
  // Animate in
  setTimeout(() => {
    toast.style.transform = 'translateX(0)';
  }, 10);
  
  // Remove after 3 seconds
  setTimeout(() => {
    toast.style.transform = 'translateX(full)';
    setTimeout(() => {
      container.removeChild(toast);
    }, 300);
  }, 3000);
}

export const toast = {
  success: (message) => showToast(message, 'success'),
  error: (message) => showToast(message, 'error'),
  warning: (message) => showToast(message, 'warning'),
  info: (message) => showToast(message, 'info')
}; 