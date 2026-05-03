import { Package } from 'lucide-react';
import Button from '../ui/Button';

export default function EmptyState({
  icon,
  title = 'Không có dữ liệu',
  description,
  actionLabel,
  onAction,
}) {
  const IconComponent = icon || Package;
  return (
    <div className="text-center py-12">
      <IconComponent className="mx-auto h-12 w-12 text-gray-400" />
      <h3 className="mt-4 text-lg font-medium text-gray-900">{title}</h3>
      {description && (
        <p className="mt-2 text-sm text-gray-500">{description}</p>
      )}
      {actionLabel && onAction && (
        <div className="mt-6">
          <Button onClick={onAction}>{actionLabel}</Button>
        </div>
      )}
    </div>
  );
}