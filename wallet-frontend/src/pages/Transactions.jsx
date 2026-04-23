import React, { useState } from 'react'

const allTransactions = [
  { id: 1, type: 'sent',     to: 'Rohit Kumar',   amount: 500,  date: '2024-04-24', time: '14:30', status: 'Success', avatar: 'RK', color: '#ef4444' },
  { id: 2, type: 'received', from: 'Priya Sharma', amount: 1200, date: '2024-04-24', time: '12:15', status: 'Success', avatar: 'PS', color: '#22c55e' },
  { id: 3, type: 'added',                          amount: 2000, date: '2024-04-23', time: '09:45', status: 'Success', avatar: 'W',  color: '#3b82f6' },
  { id: 4, type: 'sent',     to: 'Amit Patel',     amount: 300,  date: '2024-04-22', time: '18:20', status: 'Failed',  avatar: 'AP', color: '#f59e0b' },
  { id: 5, type: 'received', from: 'Sneha Gupta',  amount: 750,  date: '2024-04-22', time: '11:00', status: 'Success', avatar: 'SG', color: '#8b5cf6' },
  { id: 6, type: 'sent',     to: 'Vikram Singh',   amount: 1500, date: '2024-04-21', time: '16:30', status: 'Success', avatar: 'VS', color: '#ec4899' },
  { id: 7, type: 'added',                          amount: 5000, date: '2024-04-20', time: '10:00', status: 'Success', avatar: 'W',  color: '#3b82f6' },
  { id: 8, type: 'sent',     to: 'Meera Reddy',    amount: 850,  date: '2024-04-19', time: '14:15', status: 'Success', avatar: 'MR', color: '#06b6d4' },
]

const filters = [
  { key: 'all',      label: 'All' },
  { key: 'sent',     label: 'Sent' },
  { key: 'received', label: 'Received' },
  { key: 'added',    label: 'Added' },
]

function Transactions() {
  const [filter, setFilter] = useState('all')

  const filtered = filter === 'all'
    ? allTransactions
    : allTransactions.filter(t => t.type === filter)

  return (
    <div>
      <h1 style={{ fontSize: '22px', fontWeight: 700, color: '#dededa', letterSpacing: '-0.3px' }}>
        Transactions
      </h1>
      <p style={{ fontSize: '13px', color: '#8c8b84', marginTop: '3px', marginBottom: '20px' }}>
        View all your transaction history
      </p>

      {/* Filter tabs */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
        {filters.map(f => (
          <button
            key={f.key}
            onClick={() => setFilter(f.key)}
            className={`btn ${filter === f.key ? 'btn-primary' : 'btn-secondary'}`}
            style={{ padding: '0.5rem 1.1rem', fontSize: '13px' }}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* Transaction list */}
      <div className="stat-card2" style={{ padding: '8px 12px' }}>
        {filtered.map(tx => (
          <div key={tx.id} className="transaction-item">
            <div className="avatar" style={{ background: tx.color }}>
              {tx.avatar}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <p style={{ fontSize: '13.5px', fontWeight: 500, color: '#dededa' }}>
                {tx.type === 'sent'     ? `Sent to ${tx.to}` :
                 tx.type === 'received' ? `Received from ${tx.from}` :
                 'Added to wallet'}
              </p>
              <p style={{ fontSize: '12px', color: '#8c8b84', marginTop: '2px' }}>
                {tx.date} at {tx.time}
              </p>
            </div>
            <div style={{ textAlign: 'right', flexShrink: 0 }}>
              <p style={{
                fontSize: '14px', fontWeight: 600,
                fontFamily: "'DM Mono', monospace",
                color: tx.type === 'sent' ? '#f87171' : '#4ade80',
                marginBottom: '4px'
              }}>
                {tx.type === 'sent' ? '-' : '+'}₹{tx.amount}
              </p>
              <span className={`badge ${tx.status === 'Success' ? 'badge-success' : 'badge-failed'}`}>
                {tx.status}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default Transactions