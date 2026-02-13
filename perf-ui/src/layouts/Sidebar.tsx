import {
  BeakerIcon,
  ChartBarIcon,
  ClipboardDocumentListIcon,
  Cog6ToothIcon,
  HomeIcon,
} from '@heroicons/react/24/outline';
import { NavLink } from 'react-router-dom';

const navigation = [
  { name: 'Dashboard', to: '/', icon: HomeIcon },
  { name: 'Send Test', to: '/send', icon: BeakerIcon },
  { name: 'Test Runs', to: '/test-runs', icon: ChartBarIcon },
  { name: 'Admin', to: '/admin', icon: Cog6ToothIcon },
];

export default function Sidebar() {
  return (
    <aside className="flex h-screen w-64 flex-col bg-gray-900 text-white">
      <div className="flex h-16 items-center gap-2 px-6">
        <ClipboardDocumentListIcon className="h-7 w-7 text-blue-400" />
        <span className="text-lg font-bold tracking-tight">Perf Tester</span>
      </div>

      <nav className="mt-4 flex-1 space-y-1 px-3">
        {navigation.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-gray-800 text-white'
                  : 'text-gray-400 hover:bg-gray-800 hover:text-white'
              }`
            }
          >
            <item.icon className="h-5 w-5" />
            {item.name}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
