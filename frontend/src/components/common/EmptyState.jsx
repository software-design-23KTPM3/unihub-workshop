import { Empty } from 'antd';

export default function EmptyState({ description = 'Không có dữ liệu phù hợp.' }) {
  return <Empty className="state-card" description={description} />;
}
