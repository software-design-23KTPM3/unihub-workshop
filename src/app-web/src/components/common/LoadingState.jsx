import { Card, Skeleton } from 'antd';

export default function LoadingState({ rows = 6, card = true }) {
  const content = <Skeleton active paragraph={{ rows }} />;
  return card ? <Card className="state-card" bordered={false}>{content}</Card> : content;
}
