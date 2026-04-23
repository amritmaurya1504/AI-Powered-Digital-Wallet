import React, { useState } from 'react'

const userData = { balance: 12450 }

const recentContacts = [
  { name: 'Rohit Kumar',  avatar: 'RK', color: '#ef4444' },
  { name: 'Priya Sharma', avatar: 'PS', color: '#22c55e' },
  { name: 'Amit Patel',   avatar: 'AP', color: '#f59e0b' },
  { name: 'Sneha Gupta',  avatar: 'SG', color: '#8b5cf6' },
]

function SendMoney() {
  const [amount, setAmount] = useState('')
  const [recipient, setRecipient] = useState('')
  const [note, setNote] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    alert(`Sending ₹${amount} to ${recipient}`)
  }

  return (
    <div>
      {/* Header */}
      <h1 style={{ fontSize: '22px', fontWeight: 700, color: '#dededa', letterSpacing: '-0.3px' }}>
        Send money
      </h1>
      <p style={{ fontSize: '13px', color: '#8c8b84', marginTop: '3px', marginBottom: '20px' }}>
        Transfer money to your contacts
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>

        {/* Left — Transfer form */}
        <div className="stat-card2">
          <p style={{ fontSize: '14px', fontWeight: 600, color: '#dededa', marginBottom: '16px' }}>
            Transfer details
          </p>

          <form onSubmit={handleSubmit}>
            {/* Recipient */}
            <div style={{ marginBottom: '14px' }}>
              <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>
                Recipient
              </label>
              <input
                type="text"
                className="input-field"
                placeholder="Enter name or phone number"
                value={recipient}
                onChange={(e) => setRecipient(e.target.value)}
                required
              />
            </div>

            {/* Amount */}
            <div style={{ marginBottom: '14px' }}>
              <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>
                Amount
              </label>
              <div style={{ position: 'relative' }}>
                <span style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#8c8b84', fontSize: '14px' }}>₹</span>
                <input
                  type="number"
                  className="input-field"
                  placeholder="0.00"
                  style={{ paddingLeft: '2rem' }}
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                />
              </div>
            </div>

            {/* Note */}
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>
                Note (optional)
              </label>
              <textarea
                className="input-field"
                rows={3}
                placeholder="Add a note"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                style={{ resize: 'none' }}
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
              Send money
            </button>
          </form>

          {/* Balance pill */}
          <div style={{
            marginTop: '12px',
            padding: '10px 14px',
            background: 'rgba(59,130,246,0.08)',
            border: '1px solid rgba(59,130,246,0.18)',
            borderRadius: '8px'
          }}>
            <p style={{ fontSize: '12.5px', color: '#60a5fa' }}>
              Available balance: ₹{userData.balance.toLocaleString()}
            </p>
          </div>
        </div>

        {/* Right column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>

          {/* Recent contacts */}
          <div className="stat-card2">
            <p style={{ fontSize: '14px', fontWeight: 600, color: '#dededa', marginBottom: '12px' }}>
              Recent contacts
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
              {recentContacts.map((c, i) => (
                <div
                  key={i}
                  onClick={() => setRecipient(c.name)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: '12px',
                    padding: '9px 10px', borderRadius: '8px',
                    cursor: 'pointer', transition: 'background 0.15s'
                  }}
                  onMouseEnter={e => e.currentTarget.style.background = '#2e2d2b'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                >
                  <div className="avatar" style={{ background: c.color, width: '36px', height: '36px', fontSize: '12px' }}>
                    {c.avatar}
                  </div>
                  <p style={{ fontSize: '13.5px', fontWeight: 500, color: '#dededa' }}>{c.name}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Quick amounts */}
          <div className="stat-card2">
            <p style={{ fontSize: '14px', fontWeight: 600, color: '#dededa', marginBottom: '12px' }}>
              Quick amounts
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: '8px' }}>
              {[100, 500, 1000, 2000, 5000, 10000].map(amt => (
                <button
                  key={amt}
                  className="btn btn-secondary"
                  style={{ fontSize: '13px', padding: '10px 0' }}
                  onClick={() => setAmount(amt.toString())}
                >
                  ₹{amt}
                </button>
              ))}
            </div>
          </div>

        </div>
      </div>
    </div>
  )
}

export default SendMoney