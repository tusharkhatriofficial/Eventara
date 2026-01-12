import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getRule, enableRule, disableRule, archiveRule, testRuleById } from '../utils/api/rules';
import { RuleResponse } from '../types/rules';
import { RuleTestModal } from '../components/rules/RuleTestModal';
export const RuleDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [rule, setRule] = useState<RuleResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<any>(null);
  const [isTestOpen, setIsTestOpen] = useState(false);

  useEffect(() => {
    if (id) fetchRule(Number(id));
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function fetchRule(idNum: number) {
    try {
      setLoading(true);
      const r = await getRule(idNum);
      setRule(r);
    } catch (e: any) {
      setError(e?.message || 'Failed to fetch');
    } finally {
      setLoading(false);
    }
  }

  async function onToggle() {
    if (!rule) return;
    try {
      if (rule.status === 'ACTIVE') await disableRule(rule.id);
      else await enableRule(rule.id);
      fetchRule(rule.id);
    } catch (e: any) {
      setError(e?.message || 'Action failed');
    }
  }

  async function onArchive() {
    if (!rule) return;
    if (!confirm('Archive rule?')) return;
    try {
      await archiveRule(rule.id);
      navigate('/alerts/rules');
    } catch (e: any) {
      setError(e?.message || 'Archive failed');
    }
  }

  async function onTest() {
    if (!rule) return;
    try {
      setTesting(true);
      const res = await testRuleById(rule.id);
      setTestResult(res);
    setIsTestOpen(true);
    } catch (e: any) {
      setError(e?.message || 'Test failed');
    } finally {
      setTesting(false);
    }
  }

  if (loading) return <div className="text-center py-12">Loading...</div>;

  if (error) return <div className="text-red-600">{error}</div>;

  if (!rule) return <div className="text-gray-600">No rule selected</div>;

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{rule.name}</h1>
          <div className="text-sm text-gray-500 mt-1">{rule.description}</div>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={onTest} className="px-3 py-2 bg-yellow-100 rounded">{testing ? 'Testing...' : 'Test'}</button>
          <button onClick={onToggle} className="px-3 py-2 bg-gray-100 rounded">{rule.status === 'ACTIVE' ? 'Disable' : 'Enable'}</button>
          <Link to={`/alerts/rules/${rule.id}/edit`} className="px-3 py-2 bg-blue-600 text-white rounded">Edit</Link>
          <button onClick={onArchive} className="px-3 py-2 bg-red-100 text-red-700 rounded">Archive</button>
        </div>
      </div>

      <div className="mt-6 grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-4 rounded border border-gray-200">
          <h4 className="text-sm font-semibold text-gray-700">Status</h4>
          <div className="mt-2">{rule.status}</div>

          <h4 className="text-sm font-semibold text-gray-700 mt-4">Severity</h4>
          <div className="mt-2">{rule.severity}</div>

          <h4 className="text-sm font-semibold text-gray-700 mt-4">Priority</h4>
          <div className="mt-2">{rule.priority}</div>
        </div>

        <div className="bg-white p-4 rounded border border-gray-200 md:col-span-2">
          <h4 className="text-sm font-semibold text-gray-700">Configuration</h4>
          <pre className="mt-2 text-xs font-mono overflow-auto max-h-40">{JSON.stringify(rule.ruleConfig, null, 2)}</pre>

          <h4 className="text-sm font-semibold text-gray-700 mt-4">Notification Channels</h4>
          <div className="mt-2">{rule.notificationChannels?.join(', ') || '—'}</div>

          <h4 className="text-sm font-semibold text-gray-700 mt-4">Generated DRL</h4>
          <pre className="mt-2 text-xs font-mono overflow-auto max-h-48">{(rule as any).generatedDrl || 'No DRL retained on response'}</pre>
        </div>
      </div>

      {testResult && (
        <div className="mt-6 bg-green-50 border border-green-200 rounded p-4">
          <h4 className="font-semibold">Test result: {testResult.success ? 'OK' : 'Failed'}</h4>
          <div className="text-sm text-gray-700 mt-1">{testResult.message}</div>
          {testResult.errors && <pre className="mt-2 text-xs text-red-600">{JSON.stringify(testResult.errors, null, 2)}</pre>}
          <div className="mt-2">
            <button onClick={() => setIsTestOpen(true)} className="px-3 py-1 bg-white border rounded">Open details</button>
          </div>
        </div>
      )}

      <RuleTestModal isOpen={isTestOpen} onClose={() => setIsTestOpen(false)} result={testResult} />
    </div>
  );
};
