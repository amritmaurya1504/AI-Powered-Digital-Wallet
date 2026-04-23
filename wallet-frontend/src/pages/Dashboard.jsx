import React from 'react'
import { Link } from 'react-router-dom'

const userData = {
  name: "Amrit",
  balance: 12450,
  weekChange: 2000,
  totalSent: 4800,
  totalReceived: 8200,
  sentTransactions: 12,
  receivedTransactions: 7
}

const transactions = [
  { id: 1, type: 'sent',     to: 'Rohit Kumar', amount: 500,  time: '2 mins ago', status: 'Success', avatar: 'RK', color: '#ef4444' },
  { id: 2, type: 'received', from: 'Priya',      amount: 1200, time: '1 hr ago',   status: 'Success', avatar: 'PS', color: '#22c55e' },
  { id: 3, type: 'added',                        amount: 2000, time: 'Yesterday',  status: 'Success', avatar: 'W',  color: '#3b82f6' },
  { id: 4, type: 'sent',     to: 'Amit',         amount: 300,  time: '2 days ago', status: 'Failed',  avatar: 'AK', color: '#ef4444' },
]

const weekActivity = [
  { day: 'Mon', amount: 560 },
  { day: 'Tue', amount: 820 },
  { day: 'Wed', amount: 640 },
  { day: 'Thu', amount: 920 },
  { day: 'Fri', amount: 1200 },
  { day: 'Sat', amount: 680 },
  { day: 'Sun', amount: 780 },
]

function Dashboard() {
  const maxActivity = Math.max(...weekActivity.map(d => d.amount))
  const highlightDay = 'Fri'

  return (
    <div>
      {/* Page Header */}
      <h1 style={{ fontSize: '22px', fontWeight: 700, color: '#fbfbfb', letterSpacing: '-0.3px' }}>
        Dashboard
      </h1>
      <p style={{ fontSize: '13px', color: '#787670', marginTop: '3px', marginBottom: '20px' }}>
        Welcome back, {userData.name}
      </p>

      {/* Stat Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: '12px', marginBottom: '20px' }}>
        <div className="stat-card">
          <p style={{ fontSize: '11px', fontWeight: 600, color: '#fbfbfb', textTransform: 'uppercase', letterSpacing: '0.6px', marginBottom: '8px' }}>
            Total Balance
          </p>
          <p style={{ fontSize: '28px', fontWeight: 600, fontFamily: "'DM Mono', monospace", color: '#fbfbfb', letterSpacing: '-1px' }}>
            ₹{userData.balance.toLocaleString()}
          </p>
          <p style={{ fontSize: '12px', color: '#4ade80', marginTop: '6px' }}>
            +₹{userData.weekChange.toLocaleString()} this week
          </p>
        </div>

        <div className="stat-card">
          <p style={{ fontSize: '11px', fontWeight: 600, color: '#fbfbfb', textTransform: 'uppercase', letterSpacing: '0.6px', marginBottom: '8px' }}>
            Total Sent
          </p>
          <p style={{ fontSize: '28px', fontWeight: 600, fontFamily: "'DM Mono', monospace", color: '#f87171', letterSpacing: '-1px' }}>
            ₹{userData.totalSent.toLocaleString()}
          </p>
          <p style={{ fontSize: '12px', color: '#979796', marginTop: '6px' }}>
            {userData.sentTransactions} transactions
          </p>
        </div>

        <div className="stat-card">
          <p style={{ fontSize: '11px', fontWeight: 600, color: '#fbfbfb', textTransform: 'uppercase', letterSpacing: '0.6px', marginBottom: '8px' }}>
            Total Received
          </p>
          <p style={{ fontSize: '28px', fontWeight: 600, fontFamily: "'DM Mono', monospace", color: '#4ade80', letterSpacing: '-1px' }}>
            ₹{userData.totalReceived.toLocaleString()}
          </p>
          <p style={{ fontSize: '12px', color: '#979796', marginTop: '6px' }}>
            {userData.receivedTransactions} transactions
          </p>
        </div>
      </div>

      {/* Bottom Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 280px', gap: '16px' }}>

        {/* Recent Transactions */}
        <div className="stat-card2">
          <p style={{ fontSize: '14px', fontWeight: 600, color: '#fbfbfb', marginBottom: '12px' }}>
            Recent transactions
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
            {transactions.map(tx => (
              <div key={tx.id} className="transaction-item">
                <div className="avatar" style={{ background: tx.color }}>
                  {tx.avatar}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ fontSize: '13.5px', fontWeight: 500, color: '#fbfbfb' }}>
                    {tx.type === 'sent'     ? `Sent to ${tx.to}` :
                     tx.type === 'received' ? `Received from ${tx.from}` :
                     'Added to wallet'}
                  </p>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '3px' }}>
                    <span style={{ fontSize: '12px', color: '#979796' }}>{tx.time}</span>
                    <span className={`badge ${tx.status === 'Success' ? 'badge-success' : 'badge-failed'}`}>
                      {tx.status}
                    </span>
                  </div>
                </div>
                <p style={{
                  fontSize: '13.5px',
                  fontWeight: 600,
                  fontFamily: "'DM Mono', monospace",
                  color: tx.type === 'sent' ? '#f87171' : '#4ade80',
                  flexShrink: 0
                }}>
                  {tx.type === 'sent' ? '-' : '+'}₹{tx.amount}
                </p>
              </div>
            ))}
          </div>
        </div>

        {/* Right Column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>

          {/* Quick Actions */}
          <div className="stat-card2">
            <p style={{ fontSize: '14px', fontWeight: 600, color: '#fbfbfb', marginBottom: '12px' }}>
              Quick actions
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
              {[
                { label: 'Send',    sub: 'Transfer money', primary: true,  to: '/send' },
                { label: 'Add',     sub: 'Top up wallet',  primary: true,  to: '/add' },
                { label: 'History', sub: 'View all',       primary: false, to: '/transactions' },
                { label: 'Profile', sub: 'Settings',       primary: false, to: '/profile' },
              ].map(action => (
                <Link
                  key={action.label}
                  to={action.to}
                  className={`qa-card ${action.primary ? 'qa-card-primary' : 'qa-card-secondary'}`}
                >
                  <span style={{ fontSize: '13.5px', fontWeight: 600 }}>{action.label}</span>
                  <span style={{ fontSize: '11px', opacity: 0.6 }}>{action.sub}</span>
                  <span style={{ fontSize: '13px', marginBottom: '2px' }}>↗</span>
                </Link>
              ))}
            </div>
          </div>

          {/* Activity Chart */}
          <div className="stat-card">
            <p style={{ fontSize: '14px', fontWeight: 600, color: '#fbfbfb', marginBottom: '12px' }}>
              Activity this week
            </p>
            <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: '5px', height: '90px' }}>
              {weekActivity.map((day, i) => (
                <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flex: 1, gap: '5px', height: '100%', justifyContent: 'flex-end' }}>
                  <div
                    className={`chart-bar${day.day === highlightDay ? ' chart-bar-highlight' : ''}`}
                    style={{ width: '100%', height: `${(day.amount / maxActivity) * 100}%` }}
                    title={`₹${day.amount}`}
                  />
                  <span style={{ fontSize: '10px', color: '#979796', fontWeight: 500 }}>{day.day}</span>
                </div>
              ))}
            </div>
          </div>

        </div>
      </div>
    </div>
  )
}

export default Dashboard
