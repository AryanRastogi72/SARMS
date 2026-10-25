import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';
import { ToastContainer } from './Toast';
import { useToast } from '../hooks/useToast';
import api from '../services/api';

/**
 * Layout, full-height shell with sidebar + header + main content.
 * Passes showToast down via a React context (ToastContext) so any child
 * page can trigger notifications.
 */
import { createContext, useContext, useEffect } from 'react';

export const ToastContext = createContext(() => {});

export function useShowToast() {
  return useContext(ToastContext);
}

export default function Layout() {
  const { toasts, showToast, removeToast } = useToast();
  const { pathname } = useLocation();

  // Usage analytics (solo addition): log one page view per portal page. Skips bare "/student" etc.,
  // which only redirect. Failures are ignored so tracking can never break the page.
  useEffect(() => {
    if (pathname.split('/').filter(Boolean).length >= 2) {
      api.post('/usage/events', { route: pathname }).catch(() => {});
    }
  }, [pathname]);

  return (
    <ToastContext.Provider value={showToast}>
      <div className="flex h-screen bg-[#F5F5F5] overflow-hidden">
        {/* Sidebar (fixed width) */}
        <Sidebar />

        {/* Right column: header + scrollable content */}
        <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
          <Header />
          <main className="flex-1 overflow-y-auto px-8 py-6">
            <Outlet />
          </main>
        </div>
      </div>

      {/* Toast layer at the root */}
      <ToastContainer toasts={toasts} removeToast={removeToast} />
    </ToastContext.Provider>
  );
}
