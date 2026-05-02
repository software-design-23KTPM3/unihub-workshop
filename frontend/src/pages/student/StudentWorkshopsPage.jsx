import { Alert, Button, Col, Row, Typography, message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import EmptyState from '../../components/common/EmptyState.jsx';
import LoadingState from '../../components/common/LoadingState.jsx';
import PageHeader from '../../components/common/PageHeader.jsx';
import WorkshopCard from '../../components/workshop/WorkshopCard.jsx';
import WorkshopFilterBar from '../../components/workshop/WorkshopFilterBar.jsx';
import { getAllWorkshops } from '../../services/workshopService.js';
import { httpClient } from '../../services/httpClient.js';

export default function StudentWorkshopsPage() {
  const [workshops, setWorkshops] = useState([]);
  const [allWorkshops, setAllWorkshops] = useState([]);
  const [filters, setFilters] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [stressLoading, setStressLoading] = useState(false);

  const handleStressTest = async () => {
    setStressLoading(true);
    try {
      const result = await httpClient.get('/test/stress');
      message.success(`Stress test ok: ${result.user_id} (${result.role})`);
    } catch (err) {
      message.error(`Stress test failed: ${err.message}`);
    } finally {
      setStressLoading(false);
    }
  };

  useEffect(() => {
    let ignore = false;

    async function loadOptions() {
      const result = await getAllWorkshops();

      if (!ignore) {
        setAllWorkshops(result);
      }
    }

    loadOptions().catch(() => {
      if (!ignore) {
        setAllWorkshops([]);
      }
    });

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    let ignore = false;

    async function loadWorkshops() {
      setLoading(true);
      setError('');

      try {
        const result = await getAllWorkshops(filters);

        if (!ignore) {
          setWorkshops(result);
        }
      } catch (loadError) {
        if (!ignore) {
          setError(loadError.message);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadWorkshops();

    return () => {
      ignore = true;
    };
  }, [filters]);

  const topics = useMemo(
    () => [...new Set(allWorkshops.map((workshop) => workshop.topic))].sort(),
    [allWorkshops],
  );

  const rooms = useMemo(
    () => [...new Set(allWorkshops.map((workshop) => workshop.room))].sort(),
    [allWorkshops],
  );

  return (
    <div className="page-stack">
      <section className="student-hero">
        <div>
          <Typography.Title>Tuần lễ kỹ năng và nghề nghiệp</Typography.Title>
          <Typography.Paragraph>
            Khám phá workshop từ nhà tuyển dụng, mentor kỹ thuật và chuyên gia sản phẩm.
          </Typography.Paragraph>
        </div>
      </section>

      <PageHeader
        title="Workshop"
        eyebrow="Student Portal"
        description="Lọc theo chủ đề, ngày, phòng và trạng thái để chọn phiên phù hợp."
      />

      {/* <div style={{ padding: '0 24px', marginBottom: 16 }}>
        <Button 
          type="primary" 
          danger 
          loading={stressLoading} 
          onClick={handleStressTest}
        >
          Run Stress Test API
        </Button>
      </div> */}

      <WorkshopFilterBar
        topics={topics}
        rooms={rooms}
        loading={loading}
        onChange={setFilters}
      />

      {error && <Alert type="error" showIcon message={error} />}

      {loading ? (
        <Row gutter={[16, 16]}>
          {Array.from({ length: 6 }).map((_, index) => (
            <Col xs={24} md={12} xl={8} key={index}>
              <LoadingState rows={6} />
            </Col>
          ))}
        </Row>
      ) : workshops.length > 0 ? (
        <Row gutter={[16, 16]}>
          {workshops.map((workshop) => (
            <Col xs={24} md={12} xl={8} key={workshop.id}>
              <WorkshopCard workshop={workshop} />
            </Col>
          ))}
        </Row>
      ) : (
        <EmptyState description="Không có workshop phù hợp với bộ lọc." />
      )}
    </div>
  );
}
