import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createRule, getRule, updateRule, testRule } from '../utils/api/rules';
import { CreateRuleRequest, UpdateRuleRequest, TestRuleRequest, RuleResponse, MetricType, Condition } from '../types/rules';

// Options derived from backend enums (kept in sync with src/main/java/com/eventara/rule/enums)
const METRIC_TYPES: MetricType[] = [
  'ERROR_RATE','TOTAL_ERRORS','AVG_LATENCY','P50_LATENCY','P95_LATENCY','P99_LATENCY','MAX_LATENCY','MIN_LATENCY',
  'EVENTS_PER_SECOND','EVENTS_PER_MINUTE','EVENTS_PER_HOUR','EVENTS_PER_DAY','PEAK_THROUGHPUT','AVG_THROUGHPUT_1H','AVG_THROUGHPUT_24H',
  'EVENTS_LAST_1_MINUTE','EVENTS_LAST_5_MINUTES','EVENTS_LAST_15_MINUTES','EVENTS_LAST_1_HOUR','EVENTS_LAST_24_HOURS','TOTAL_EVENTS',
  'UNIQUE_SOURCES','UNIQUE_EVENT_TYPES','UNIQUE_USERS','SYSTEM_HEALTH','ACTIVE_USERS_LAST_1_HOUR','ACTIVE_USERS_LAST_24_HOURS','TOTAL_UNIQUE_USERS'
];

const CONDITIONS: Condition[] = [
  'GREATER_THAN','LESS_THAN','EQUALS','GREATER_THAN_OR_EQUAL','LESS_THAN_OR_EQUAL','NOT_EQUALS','BETWEEN','NOT_BETWEEN'
];
import { RuleTestModal } from '../components/rules/RuleTestModal';

export const RuleEditor: React.FC = () => {
  const { id } = useParams<{ id?: string }>();
  const isEdit = !!id && id !== 'new';
  const navigate = useNavigate();

  const [form, setForm] = useState<CreateRuleRequest | UpdateRuleRequest>({
    name: '',
    description: '',
    ruleType: 'THRESHOLD',
    ruleConfig: {},
    severity: 'CRITICAL',
    priority: 0,
  } as any);
  const [jsonMode, setJsonMode] = useState(false);
  const [jsonText, setJsonText] = useState('');
  const [loading, setLoading] = useState(false);
  const [testResult, setTestResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [isTestOpen, setIsTestOpen] = useState(false);

  useEffect(() => {
    if (isEdit && id) fetchRule(Number(id));
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function fetchRule(id: number) {
    try {
      setLoading(true);
      const r: RuleResponse = await getRule(id);
      setForm({
        name: r.name,
        description: r.description,
        ruleType: r.ruleType,
        ruleConfig: r.ruleConfig,
        severity: r.severity,
        priority: r.priority,
        notificationChannels: r.notificationChannels,
        notificationConfig: r.notificationConfig,
        suppressionWindowMinutes: r.suppressionWindowMinutes,
        maxAlertsPerHour: r.maxAlertsPerHour,
      } as any);
      setJsonText(JSON.stringify(r.ruleConfig || {}, null, 2));
    } catch (e: any) {
      setError(e?.message || 'Failed to load rule');
    } finally {
      setLoading(false);
    }
  }

  async function onTest() {
    try {
      setError(null);

      const cfg = jsonMode ? JSON.parse(jsonText) : (form as any).ruleConfig;
      // Client-side validation that mirrors backend requirements
      if (!cfg || !cfg.metricType) throw new Error('metricType is required');
      if (!cfg.condition) throw new Error('condition is required');
      if (cfg.thresholdValue === undefined || cfg.thresholdValue === '') throw new Error('thresholdValue is required');
      if (cfg.timeWindowMinutes !== undefined && (!Number.isInteger(cfg.timeWindowMinutes) || cfg.timeWindowMinutes <= 0)) {
        throw new Error('timeWindowMinutes must be a positive integer');
      }

      const payload: TestRuleRequest = {
        ruleType: (form as any).ruleType,
        ruleConfig: cfg,
        severity: (form as any).severity,
        priority: (form as any).priority,
        name: (form as any).name,
      };
      const res = await testRule(payload);
      setTestResult(res);
    } catch (e: any) {
      setError(e?.message || 'Test failed');
    }
  }

  async function onSave() {
    try {
      setLoading(true);
      setError(null);

      const cfg = jsonMode ? JSON.parse(jsonText) : (form as any).ruleConfig;
      // Basic validation before save
      if (!cfg || !cfg.metricType) throw new Error('metricType is required');
      if (!cfg.condition) throw new Error('condition is required');
      if (cfg.thresholdValue === undefined || cfg.thresholdValue === '') throw new Error('thresholdValue is required');
      if (cfg.timeWindowMinutes !== undefined && (!Number.isInteger(cfg.timeWindowMinutes) || cfg.timeWindowMinutes <= 0)) {
        throw new Error('timeWindowMinutes must be a positive integer');
      }

      const payload: CreateRuleRequest | UpdateRuleRequest = {
        ...(form as any),
        ruleConfig: cfg,
      } as any;

      if (isEdit && id) {
        await updateRule(Number(id), payload as UpdateRuleRequest);
      } else {
        await createRule(payload as CreateRuleRequest);
      }
      navigate('/alerts/rules');
    } catch (e: any) {
      setError(e?.message || 'Save failed');
    } finally {
      setLoading(false);
    }
  }

  function onField<K extends keyof typeof form>(key: K, value: any) {
    setForm((f: any) => {
      const next = { ...f, [key]: value } as any;
      // Keep JSON editor in sync when editing the form mode
      if (key === 'ruleConfig' && !jsonMode) {
        try {
          setJsonText(JSON.stringify(next.ruleConfig || {}, null, 2));
        } catch (e) {
          // ignore
        }
      }
      return next;
    });
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{isEdit ? 'Edit rule' : 'Create rule'}</h1>
          <p className="text-sm text-gray-500 mt-1">Define the rule criteria and notification behavior.</p>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={onTest} className="px-3 py-2 bg-yellow-100 rounded">Test</button>
          <button onClick={onSave} className="px-4 py-2 bg-blue-600 text-white rounded">{loading ? 'Saving...' : 'Save'}</button>
        </div>
      </div>

      {error && <div className="text-red-600 mt-4">{error}</div>}

      <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <label className="block text-sm font-medium text-gray-700">Name</label>
          <input className="mt-1 block w-full border rounded p-2" value={(form as any).name} onChange={(e) => onField('name', e.target.value)} />

          <label className="block text-sm font-medium text-gray-700 mt-4">Description</label>
          <textarea className="mt-1 block w-full border rounded p-2" value={(form as any).description} onChange={(e) => onField('description', e.target.value)} />

          <label className="block text-sm font-medium text-gray-700 mt-4">Rule type</label>
          <select className="mt-1 block w-full border rounded p-2" value={(form as any).ruleType} onChange={(e) => onField('ruleType', e.target.value)}>
            <option value="THRESHOLD">Threshold</option>
            <option value="PATTERN">Pattern</option>
            <option value="ANOMALY">Anomaly</option>
            <option value="CEP">CEP</option>
          </select>

          <label className="block text-sm font-medium text-gray-700 mt-4">Severity</label>
          <select className="mt-1 block w-full border rounded p-2" value={(form as any).severity} onChange={(e) => onField('severity', e.target.value)}>
            <option>CRITICAL</option>
            <option>WARNING</option>
            <option>INFO</option>
          </select>

          <label className="block text-sm font-medium text-gray-700 mt-4">Priority</label>
          <input type="number" className="mt-1 block w-full border rounded p-2" value={(form as any).priority ?? 0} onChange={(e) => onField('priority', Number(e.target.value))} />
        </div>

        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold">Rule configuration</h3>
            <div className="space-x-2">
              <button onClick={() => setJsonMode((s) => !s)} className="px-2 py-1 bg-gray-100 rounded">{jsonMode ? 'Form' : 'JSON'}</button>
            </div>
          </div>

          {!jsonMode && (
            <div className="mt-3">
              <label className="block text-sm font-medium text-gray-700">Metric Type</label>
              <select className="mt-1 block w-full border rounded p-2" value={(form as any).ruleConfig?.metricType || ''} onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, metricType: e.target.value })}>
                <option value="">Select metric</option>
                {METRIC_TYPES.map((m) => (
                  <option key={m} value={m}>{m.replaceAll('_', ' ')}</option>
                ))}
              </select>
              {!((form as any).ruleConfig?.metricType) && <div className="text-xs text-red-600 mt-1">metricType is required</div>}

              <label className="block text-sm font-medium text-gray-700 mt-3">Condition</label>
              <select className="mt-1 block w-full border rounded p-2" value={(form as any).ruleConfig?.condition || ''} onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, condition: e.target.value })}>
                <option value="">Select condition</option>
                {CONDITIONS.map((c) => (
                  <option key={c} value={c}>{c.replaceAll('_', ' ')}</option>
                ))}
              </select>
              {!((form as any).ruleConfig?.condition) && <div className="text-xs text-red-600 mt-1">condition is required</div>}

              <label className="block text-sm font-medium text-gray-700 mt-3">Threshold value</label>
              <input className="mt-1 block w-full border rounded p-2" value={(form as any).ruleConfig?.thresholdValue || ''} onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, thresholdValue: e.target.value })} />
              {((form as any).ruleConfig?.thresholdValue === undefined || (form as any).ruleConfig?.thresholdValue === '') && <div className="text-xs text-gray-500 mt-1">Threshold is required by the backend; any value is allowed (numeric or string for some metrics).</div>}

              <label className="block text-sm font-medium text-gray-700 mt-3">Time window (minutes)</label>
              <input type="number" min={1} className="mt-1 block w-full border rounded p-2" value={(form as any).ruleConfig?.timeWindowMinutes || ''} onChange={(e) => onField('ruleConfig', { ...(form as any).ruleConfig, timeWindowMinutes: e.target.value ? Number(e.target.value) : undefined })} />
              {((form as any).ruleConfig?.timeWindowMinutes !== undefined && ((form as any).ruleConfig?.timeWindowMinutes <= 0 || !Number.isInteger((form as any).ruleConfig?.timeWindowMinutes))) && <div className="text-xs text-red-600 mt-1">timeWindowMinutes must be a positive integer</div>}
            </div>
          )}

          {jsonMode && (
            <div className="mt-3">
              <label className="block text-sm font-medium text-gray-700">Rule config (JSON)</label>
              <textarea className="mt-1 block w-full border rounded p-2 font-mono text-sm h-56" value={jsonText} onChange={(e) => setJsonText(e.target.value)} />
            </div>
          )}

          {testResult && (
            <div className="mt-4 p-3 bg-gray-50 rounded">
              <div className="text-sm font-medium">Test result: {testResult.success ? 'OK' : 'Failed'}</div>
              {testResult.message && <div className="text-xs text-gray-600 mt-2">{testResult.message}</div>}
              {testResult.errors && <pre className="mt-2 text-xs text-red-600">{JSON.stringify(testResult.errors, null, 2)}</pre>}
              <div className="mt-2">
                <button onClick={() => setIsTestOpen(true)} className="px-3 py-1 bg-white border rounded">Open details</button>
              </div>
            </div>
          )}

      {/* Test result modal */}
      <RuleTestModal isOpen={isTestOpen} onClose={() => setIsTestOpen(false)} result={testResult} />
        </div>
      </div>
    </div>
  );
};
