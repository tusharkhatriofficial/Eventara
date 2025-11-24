import { useState } from 'react'
import { ComprehensiveMetrics } from './types'
import './App.css'

function App() {
  const [metrics, setMetrics] = useState<ComprehensiveMetrics | null>(null)

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">
          Eventara Dashboard
        </h1>
        <p className="text-gray-600">
          Real-time Event Analytics Platform
        </p>
        
        <div className="mt-8 p-4 bg-white rounded-lg shadow">
          <p className="text-sm text-gray-500">
            Metrics: {metrics ? 'Loaded' : 'Not loaded yet'}
          </p>
        </div>
      </div>
    </div>
  )
}

export default App
