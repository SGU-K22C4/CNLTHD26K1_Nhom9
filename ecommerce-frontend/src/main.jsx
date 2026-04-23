import { StrictMode, Component } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './shared/styles/globals.css'
import App from './App.jsx'
import { AuthProvider } from './modules/auth/context/AuthContext';

// Error boundary to catch silent React errors
class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }
  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }
  componentDidCatch(error, info) {
    console.error('[ErrorBoundary] React render error:', error, info);
  }
  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: '40px', color: 'red', fontSize: '18px' }}>
          <h1>App Error</h1>
          <pre>{this.state.error?.message}</pre>
          <pre>{this.state.error?.stack}</pre>
        </div>
      );
    }
    return this.props.children;
  }
}

console.log('[main.jsx] Starting React app...');
console.log('[main.jsx] API URL:', import.meta.env.VITE_API_BASE_URL);

try {
  const root = createRoot(document.getElementById('root'));
  console.log('[main.jsx] Root created, rendering...');
  root.render(
    <StrictMode>
      <ErrorBoundary>
        <BrowserRouter>
          <AuthProvider>
            <App />
          </AuthProvider>
        </BrowserRouter>
      </ErrorBoundary>
    </StrictMode>,
  );
  console.log('[main.jsx] Render called successfully');
} catch (err) {
  console.error('[main.jsx] FATAL ERROR:', err);
  document.getElementById('root').innerHTML = 
    '<div style="color:red;padding:40px"><h1>Fatal Error</h1><pre>' + err.message + '</pre><pre>' + err.stack + '</pre></div>';
}
