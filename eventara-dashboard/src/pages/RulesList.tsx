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
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gradient">Alert Rules</h1>
          <p className="text-sm text-dark-600 mt-2 flex items-center gap-2">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Create and manage alert rules that trigger notifications when conditions are met
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Link to="/alerts/rules/new" className="btn-primary flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Create Rule
          </Link>
        </div>
      </div>

      {/* Stats Cards */}
      {!loading && rules && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 animate-slide-up">
          <div className="stat-card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-dark-600">Total Rules</p>
                <p className="text-2xl font-bold text-dark-900 mt-1">{rules.length}</p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-500/30">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
            </div>
          </div>

          <div className="stat-card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-dark-600">Active Rules</p>
                <p className="text-2xl font-bold text-emerald-600 mt-1">{rules.filter(r => r.status === 'ACTIVE').length}</p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-600 flex items-center justify-center text-white shadow-lg shadow-emerald-500/30">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </div>

          <div className="stat-card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-dark-600">Inactive Rules</p>
                <p className="text-2xl font-bold text-amber-600 mt-1">{rules.filter(r => r.status !== 'ACTIVE').length}</p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-amber-500 to-amber-600 flex items-center justify-center text-white shadow-lg shadow-amber-500/30">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </div>

          <div className="stat-card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-dark-600">Recently Triggered</p>
                <p className="text-2xl font-bold text-purple-600 mt-1">{rules.filter(r => r.lastTriggeredAt).length}</p>
              </div>
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-500 to-purple-600 flex items-center justify-center text-white shadow-lg shadow-purple-500/30">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Content */}
      <div className="card-gradient p-8 min-h-[400px]">
        {loading && (
          <div className="flex flex-col items-center justify-center py-16 animate-pulse">
            <div className="w-16 h-16 rounded-full bg-gradient-to-tr from-primary-500 to-primary-700 animate-spin mb-6 flex items-center justify-center">
              <div className="w-12 h-12 rounded-full bg-white"></div>
            </div>
            <p className="text-dark-600 font-medium">Loading rules...</p>
            <p className="text-sm text-dark-500 mt-1">Please wait</p>
          </div>
        )}

        {error && (
          <div className="bg-gradient-to-r from-red-50 to-rose-50 border-l-4 border-red-500 p-6 rounded-xl flex items-start gap-4">
            <svg className="w-6 h-6 text-red-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <h3 className="font-semibold text-red-900">Error loading rules</h3>
              <p className="text-red-700 mt-1">{error}</p>
            </div>
          </div>
        )}

        {!loading && rules && rules.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16">
            <div className="w-24 h-24 rounded-2xl bg-gradient-to-br from-dark-100 to-dark-200 flex items-center justify-center mb-6">
              <svg className="w-12 h-12 text-dark-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <h3 className="text-xl font-semibold text-dark-900 mb-2">No rules yet</h3>
            <p className="text-dark-600 mb-6 text-center max-w-md">
              Get started by creating your first alert rule. Rules help you monitor metrics and trigger notifications automatically.
            </p>
            <Link to="/alerts/rules/new" className="btn-primary flex items-center gap-2">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              Create Your First Rule
            </Link>
          </div>
        )}

        {!loading && rules && rules.length > 0 && (
          <div className="overflow-hidden rounded-xl border border-dark-100 animate-slide-up">
            <table className="table-modern">
              <thead>
                <tr>
                  <th>Rule Name</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Severity</th>
                  <th>Last Triggered</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {rules.map((r) => (
                  <tr key={r.id} className="hover:bg-primary-50/30 transition-colors">
                    <td>
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-100 to-primary-200 flex items-center justify-center">
                          <span className="text-sm font-bold text-primary-700">{r.name.substring(0, 2).toUpperCase()}</span>
                        </div>
                        <div>
                          <p className="font-semibold text-dark-900">{r.name}</p>
                          <p className="text-xs text-dark-500 mt-0.5">{r.description?.substring(0, 50) || 'No description'}</p>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className="badge badge-info">{r.ruleType}</span>
                    </td>
                    <td>
                      {r.status === 'ACTIVE' ? (
                        <span className="badge badge-success flex items-center gap-1.5 w-fit">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                          Active
                        </span>
                      ) : (
                        <span className="badge bg-dark-100 text-dark-600">Inactive</span>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${
                        r.severity === 'CRITICAL' ? 'badge-error' : 
                        r.severity === 'WARNING' ? 'badge-warning' : 
                        'badge-info'
                      }`}>
                        {r.severity}
                      </span>
                    </td>
                    <td>
                      <div className="flex items-center gap-2 text-dark-600">
                        {r.lastTriggeredAt ? (
                          <>
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <span className="text-sm">{new Date(r.lastTriggeredAt).toLocaleString()}</span>
                          </>
                        ) : (
                          <span className="text-sm text-dark-400">Never</span>
                        )}
                      </div>
                    </td>
                    <td>
                      <div className="flex items-center justify-end gap-2">
                        <Link 
                          to={`/alerts/rules/${r.id}`} 
                          className="btn-ghost text-xs"
                          title="View details"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>
                        </Link>
                        <button 
                          onClick={() => onToggle(r)} 
                          className={`btn-ghost text-xs ${r.status === 'ACTIVE' ? 'text-amber-600' : 'text-emerald-600'}`}
                          title={r.status === 'ACTIVE' ? 'Disable' : 'Enable'}
                        >
                          {r.status === 'ACTIVE' ? (
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                          ) : (
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                          )}
                        </button>
                        <Link 
                          to={`/alerts/rules/${r.id}/edit`} 
                          className="btn-ghost text-xs text-primary-600"
                          title="Edit rule"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                          </svg>
                        </Link>
                        <button 
                          onClick={() => onDelete(r.id)} 
                          className="btn-ghost text-xs text-red-600"
                          title="Delete rule"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      </div>
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
