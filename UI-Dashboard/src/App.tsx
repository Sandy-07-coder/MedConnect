import { useEffect, useState } from 'react';
import { Activity, AlertTriangle, ArrowRightLeft, Clock, Search, ShieldCheck } from 'lucide-react';
import type { DashboardSummary, FacilityAnalysis, Medicine } from './types';

function App() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [analysis, setAnalysis] = useState<FacilityAnalysis[]>([]);
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  
  // Filters
  const [selectedMedicine, setSelectedMedicine] = useState<string>('');
  const [district, setDistrict] = useState<string>('Vellore');
  const [horizon, setHorizon] = useState<number>(14);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchMedicines();
    fetchSummary();
    fetchAnalysis();
  }, []);

  const fetchMedicines = async () => {
    try {
      const res = await fetch('http://localhost:8081/api/medicines');
      if (!res.ok) throw new Error('Failed to fetch medicines');
      const data = await res.json();
      setMedicines(data);
      if (data.length > 0) {
        setSelectedMedicine(data[0].id.toString());
      }
    } catch (err) {
      console.error(err);
    }
  };

  const fetchSummary = async () => {
    try {
      const res = await fetch('http://localhost:8082/api/dashboard/summary');
      if (!res.ok) throw new Error('Forecast Service unreachable');
      const data = await res.json();
      setSummary(data);
    } catch (err: any) {
      setError(err.message || 'Error fetching summary');
    }
  };

  const fetchAnalysis = async (medId?: string) => {
    setLoading(true);
    setError('');
    const mId = medId || selectedMedicine;
    
    try {
      // Build query params
      const params = new URLSearchParams();
      if (district) params.append('district', district);
      if (mId) params.append('medicineId', mId);
      if (horizon) params.append('forecastHorizonDays', horizon.toString());

      const res = await fetch(`http://localhost:8082/api/analysis?${params.toString()}`);
      if (!res.ok) throw new Error('Forecast Service unreachable');
      const data = await res.json();
      setAnalysis(data);
    } catch (err: any) {
      setError(err.message || 'Error fetching analysis');
      setAnalysis([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    fetchSummary();
    fetchAnalysis();
  };

  const getRiskColor = (level: string) => {
    switch (level) {
      case 'CRITICAL': return 'bg-red-100 text-red-800 border-red-200';
      case 'HIGH': return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      default: return 'bg-emerald-100 text-emerald-800 border-emerald-200';
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 font-sans">
      {/* HEADER */}
      <header className="bg-emerald-800 text-white shadow-md">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Activity className="h-6 w-6 text-emerald-300" />
            <h1 className="text-xl font-bold tracking-tight">MediConnect</h1>
          </div>
          <div className="text-sm text-emerald-200 font-medium flex items-center gap-2">
            <ShieldCheck className="h-4 w-4" /> Operations Dashboard
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-8">
        
        {/* SUMMARY CARDS */}
        {summary && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
            <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
              <span className="text-sm font-semibold text-slate-500 uppercase tracking-wider">Facilities</span>
              <div className="mt-2 text-3xl font-bold text-slate-800">{summary.facilitiesMonitored}</div>
            </div>
            <div className="bg-white rounded-xl p-5 border border-red-100 shadow-sm flex flex-col justify-between relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10 text-red-600"><AlertTriangle size={48} /></div>
              <span className="text-sm font-semibold text-red-600 uppercase tracking-wider relative z-10">Stock-out Risk</span>
              <div className="mt-2 text-3xl font-bold text-red-700 relative z-10">{summary.facilitiesAtRisk}</div>
            </div>
            <div className="bg-white rounded-xl p-5 border border-orange-100 shadow-sm flex flex-col justify-between relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10 text-orange-600"><Clock size={48} /></div>
              <span className="text-sm font-semibold text-orange-600 uppercase tracking-wider relative z-10">Expiry Risk</span>
              <div className="mt-2 text-3xl font-bold text-orange-700 relative z-10">{summary.batchesWithExpiryRisk}</div>
            </div>
            <div className="bg-white rounded-xl p-5 border border-emerald-100 shadow-sm flex flex-col justify-between relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10 text-emerald-600"><ArrowRightLeft size={48} /></div>
              <span className="text-sm font-semibold text-emerald-600 uppercase tracking-wider relative z-10">Pending Transfers</span>
              <div className="mt-2 text-3xl font-bold text-emerald-700 relative z-10">{summary.pendingRedistributionRecommendations}</div>
            </div>
          </div>
        )}

        {/* FILTERS */}
        <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-200 mb-8 flex flex-wrap gap-4 items-end">
          <div className="flex-1 min-w-[200px]">
            <label className="block text-sm font-medium text-slate-600 mb-1">District</label>
            <input 
              type="text" 
              value={district}
              onChange={(e) => setDistrict(e.target.value)}
              className="w-full rounded-lg border-slate-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 bg-slate-50 border p-2 text-sm"
            />
          </div>
          <div className="flex-1 min-w-[200px]">
            <label className="block text-sm font-medium text-slate-600 mb-1">Medicine</label>
            <select 
              value={selectedMedicine}
              onChange={(e) => {
                setSelectedMedicine(e.target.value);
                fetchAnalysis(e.target.value);
              }}
              className="w-full rounded-lg border-slate-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 bg-slate-50 border p-2 text-sm"
            >
              {medicines.map(m => (
                <option key={m.id} value={m.id}>{m.name}</option>
              ))}
            </select>
          </div>
          <div className="w-[150px]">
            <label className="block text-sm font-medium text-slate-600 mb-1">Horizon (Days)</label>
            <input 
              type="number" 
              value={horizon}
              onChange={(e) => setHorizon(parseInt(e.target.value))}
              className="w-full rounded-lg border-slate-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 bg-slate-50 border p-2 text-sm"
            />
          </div>
          <button 
            onClick={handleSearch}
            disabled={loading}
            className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-2 rounded-lg font-medium transition-colors flex items-center gap-2 disabled:opacity-50 h-[42px]"
          >
            <Search size={16} />
            {loading ? 'Analyzing...' : 'Analyze'}
          </button>
        </div>

        {error && (
          <div className="mb-8 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 flex items-center gap-3">
            <AlertTriangle className="h-5 w-5 flex-shrink-0" />
            <p className="text-sm font-medium">{error}</p>
          </div>
        )}

        {/* RESULTS */}
        {analysis.length > 0 && !loading && (
          <div className="space-y-6">
            <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
              Facility Analysis
            </h2>
            
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              
              {/* MAIN TABLE COLUMN */}
              <div className="lg:col-span-2 space-y-6">
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                  <table className="min-w-full divide-y divide-slate-200">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Facility</th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Stock</th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Demand/Day</th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Days Left</th>
                        <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Risk Level</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-slate-200">
                      {analysis.map((row) => (
                        <tr key={row.facilityId} className="hover:bg-slate-50 transition-colors">
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="font-medium text-slate-900">{row.facilityName}</div>
                            <div className="text-xs text-slate-500">{row.district}</div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap font-medium">{row.usableStock}</td>
                          <td className="px-6 py-4 whitespace-nowrap text-slate-600">{row.forecastDailyDemand.toFixed(1)}</td>
                          <td className="px-6 py-4 whitespace-nowrap font-medium">
                            {row.daysToStockOut > 999 ? '∞' : row.daysToStockOut.toFixed(1)}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <span className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full border ${getRiskColor(row.riskLevel)}`}>
                              {row.riskLevel}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* EXPLANATIONS */}
                <div className="bg-slate-50 rounded-xl p-4 border border-slate-200">
                  <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wider mb-3 flex items-center gap-2">
                    Analysis Rationale
                  </h3>
                  <div className="space-y-2">
                    {analysis.map(row => (
                      <div key={row.facilityId} className="text-sm text-slate-600">
                        <span className="font-medium text-slate-800">{row.facilityName}:</span> {row.analysisExplanation}
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* SIDEBAR PANELS */}
              <div className="space-y-6">
                
                {/* REDISTRIBUTION PANEL */}
                <div className="bg-white rounded-xl shadow-sm border border-emerald-200 overflow-hidden">
                  <div className="bg-emerald-50 px-4 py-3 border-b border-emerald-100 flex items-center gap-2">
                    <ArrowRightLeft className="text-emerald-600 h-5 w-5" />
                    <h3 className="font-semibold text-emerald-800">Redistribution Recommendations</h3>
                  </div>
                  <div className="p-4 space-y-4">
                    {analysis.filter(a => a.redistribution).length === 0 ? (
                      <p className="text-sm text-slate-500 italic">No transfers recommended.</p>
                    ) : (
                      analysis.filter(a => a.redistribution).map(row => {
                        const rec = row.redistribution!;
                        return (
                          <div key={row.facilityId} className="border border-slate-100 bg-slate-50 rounded-lg p-3 text-sm">
                            <div className="flex justify-between items-center mb-2">
                              <span className="font-bold text-slate-800">{rec.donorFacilityName}</span>
                              <ArrowRightLeft size={14} className="text-slate-400" />
                              <span className="font-bold text-slate-800">{row.facilityName}</span>
                            </div>
                            <div className="flex justify-between items-center mb-2">
                              <span className="text-xs text-slate-500">Distance: {rec.estimatedDistanceKm.toFixed(1)} km</span>
                              <span className="bg-emerald-100 text-emerald-800 font-bold px-2 py-1 rounded text-xs">
                                {rec.recommendedTransferQuantity} units
                              </span>
                            </div>
                            <p className="text-xs text-slate-600 mt-2 border-t border-slate-200 pt-2 leading-relaxed">
                              {rec.explanation}
                            </p>
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>

                {/* EXPIRY PANEL */}
                <div className="bg-white rounded-xl shadow-sm border border-orange-200 overflow-hidden">
                  <div className="bg-orange-50 px-4 py-3 border-b border-orange-100 flex items-center gap-2">
                    <Clock className="text-orange-600 h-5 w-5" />
                    <h3 className="font-semibold text-orange-800">Expiry Risk Alerts</h3>
                  </div>
                  <div className="p-4 space-y-4">
                    {analysis.flatMap(a => a.expiryAlerts.map(alert => ({ ...alert, facilityName: a.facilityName }))).length === 0 ? (
                      <p className="text-sm text-slate-500 italic">No batches at risk of expiry.</p>
                    ) : (
                      analysis.flatMap(a => a.expiryAlerts.map(alert => ({ ...alert, facilityName: a.facilityName }))).map((alert, idx) => (
                        <div key={idx} className="border border-orange-100 bg-orange-50/50 rounded-lg p-3 text-sm">
                          <div className="flex justify-between items-center mb-1">
                            <span className="font-semibold text-slate-800">{alert.facilityName}</span>
                            <span className="text-xs font-mono bg-white px-1.5 py-0.5 rounded border border-slate-200">{alert.batchNumber}</span>
                          </div>
                          <div className="text-xs text-slate-600 mb-2">
                            Expires: <span className="font-semibold text-red-600">{alert.expiryDate}</span> ({alert.daysUntilExpiry} days)
                          </div>
                          <div className="text-xs text-slate-600 mb-2">
                            Est. At Risk: <span className="font-bold text-orange-700">{alert.estimatedQuantityAtRisk} units</span>
                          </div>
                          <p className="text-xs text-slate-600 border-t border-orange-100 pt-2 leading-relaxed">
                            {alert.explanation}
                          </p>
                        </div>
                      ))
                    )}
                  </div>
                </div>

              </div>
            </div>
          </div>
        )}

      </main>
    </div>
  );
}

export default App;
