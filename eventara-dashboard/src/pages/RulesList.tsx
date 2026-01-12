import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listRules, enableRule, disableRule, deleteRule } from '../utils/api/rules';
import { RuleResponse } from '../types/rules';

export const RulesList: React.FC = () => {
  const [rules, setRules] = useState<RuleResponse[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchRules();
  }, []);

  async function fetchRules() {
    setLoading(true);
    try {
      const data = await listRules();
      setRules(data);
    } catch (e: any) {
      setError(e?.message || 'Failed to fetch rules');
    } finally {
      setLoading(false);
    }
  }

  async function onToggle(rule: RuleResponse) {
    try {
      if (rule.status === 'ACTIVE') {
        await disableRule(rule.id);
      } else {
        await enableRule(rule.id);
      }
      fetchRules();
    } catch (e: any) {
      setError(e?.message || 'Action failed');
    }
  }

  async function onDelete(id: number) {
    if (!confirm('Delete rule? This action cannot be undone.')) return;
    try {
      await deleteRule(id);
      fetchRules();
    } catch (e: any) {
      setError(e?.message || 'Failed to delete');
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Alert Rules</h1>
          <p className="text-sm text-gray-500 mt-1">Create and manage alert rules that trigger notifications.</p>
        </div>
        <div className="flex items-center gap-3">
          <Link to="/alerts/rules/new" className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
            Create rule
          </Link>
        </div>
      </div>

      <div className="mt-6">
        {loading && (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600 mb-4"></div>
            <div className="text-sm text-gray-600">Loading rules...</div>
          </div>
        )}

        {error && <div className="text-red-600">{error}</div>}

        {!loading && rules && rules.length === 0 && (
          <div className="bg-white rounded-lg border border-dashed border-gray-200 p-8 text-center">
            <p className="text-gray-700 mb-4">No custom rules yet.</p>
            <Link to="/alerts/rules/new" className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
              Create your first rule
            </Link>
          </div>
        )}

        {!loading && rules && rules.length > 0 && (
          <div className="mt-4 overflow-x-auto bg-white rounded-lg border border-gray-200">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Severity</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Last triggered</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {rules.map((r) => (
                  <tr key={r.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{r.name}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{r.ruleType}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${r.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-700'}`}>
                        {r.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{r.severity}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{r.lastTriggeredAt ? new Date(r.lastTriggeredAt).toLocaleString() : '—'}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium flex items-center justify-end gap-2">
                      <Link to={`/alerts/rules/${r.id}`} className="text-blue-600 hover:underline">View</Link>
                      <button onClick={() => onToggle(r)} className="px-2 py-1 rounded bg-gray-100 text-sm">{r.status === 'ACTIVE' ? 'Disable' : 'Enable'}</button>
                      <Link to={`/alerts/rules/${r.id}/edit`} className="px-2 py-1 rounded bg-gray-50 text-sm">Edit</Link>
                      <button onClick={() => onDelete(r.id)} className="px-2 py-1 rounded bg-red-100 text-sm text-red-700">Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
