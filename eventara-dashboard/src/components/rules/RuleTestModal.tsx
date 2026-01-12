import React from 'react';
import { RuleTestResult } from '../../types/rules';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  result?: RuleTestResult | null;
}

export const RuleTestModal: React.FC<Props> = ({ isOpen, onClose, result }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-lg w-11/12 max-w-2xl p-6">
        <div className="flex items-start justify-between">
          <h3 className="text-lg font-bold">Rule test result</h3>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-800">Close</button>
        </div>

        {!result && <div className="mt-4 text-gray-600">No result available.</div>}

        {result && (
          <div className="mt-4 space-y-3 text-sm">
            <div className="font-medium">Status: <span className={`ml-2 ${result.success ? 'text-green-600' : 'text-red-600'}`}>{result.success ? 'Success' : 'Failed'}</span></div>
            {result.message && <div><strong>Message:</strong> <div className="text-gray-700">{result.message}</div></div>}
            {result.generatedDrl && (
              <div>
                <strong>Generated DRL:</strong>
                <pre className="mt-2 bg-gray-50 p-3 rounded text-xs font-mono max-h-56 overflow-auto">{result.generatedDrl}</pre>
              </div>
            )}
            {result.errors && result.errors.length > 0 && (
              <div>
                <strong>Errors:</strong>
                <pre className="mt-2 text-xs text-red-600 bg-red-50 p-2 rounded">{JSON.stringify(result.errors, null, 2)}</pre>
              </div>
            )}
            {result.warnings && result.warnings.length > 0 && (
              <div>
                <strong>Warnings:</strong>
                <pre className="mt-2 text-xs text-yellow-700 bg-yellow-50 p-2 rounded">{JSON.stringify(result.warnings, null, 2)}</pre>
              </div>
            )}
          </div>
        )}

        <div className="mt-6 flex justify-end">
          <button onClick={onClose} className="px-4 py-2 bg-gray-100 rounded">Close</button>
        </div>
      </div>
    </div>
  );
};
