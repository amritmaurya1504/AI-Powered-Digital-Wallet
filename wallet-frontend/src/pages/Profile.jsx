import React, { useState } from 'react'

const userData = {
  name: "Amrit",
  userId: "user-001",
  balance: 12450,
  totalSent: 4800,
  totalReceived: 8200,
  sentTransactions: 12,
  receivedTransactions: 7
}

const Toggle = ({ defaultChecked }) => {
  const [on, setOn] = useState(defaultChecked || false)
  return (
    <div
      onClick={() => setOn(!on)}
      style={{
        width: '36px', height: '20px', borderRadius: '20px',
        background: on ? '#3b82f6' : '#3a3a38',
        border: `1px solid ${on ? '#2d5a8e' : '#4e4e4e'}`,
        position: 'relative', cursor: 'pointer',
        transition: 'all 0.2s', flexShrink: 0
      }}
    >
      <div style={{
        width: '14px', height: '14px', borderRadius: '50%',
        background: on ? '#fff' : '#8c8b84',
        position: 'absolute', top: '2px',
        left: on ? '18px' : '2px',
        transition: 'all 0.2s'
      }} />
    </div>
  )
}

function Profile() {
  const handleSubmit = (e) => {
    e.preventDefault()
    alert('Profile updated successfully!')
  }

  const statRow = (label, value, color) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid #3a3a38' }}>
      <span style={{ fontSize: '13.5px', color: '#8c8b84' }}>{label}</span>
      <span style={{ fontSize: '14px', fontWeight: 600, fontFamily: "'DM Mono', monospace", color: color || '#dededa' }}>
        {value}
      </span>
    </div>
  )

  const secBtn = (label) => (
    <button
      className="btn btn-secondary"
      style={{ width: '100%', justifyContent: 'flex-start', fontSize: '13px', padding: '10px 14px' }}
    >
      {label}
    </button>
  )

  return (
    <div>
      {/* Header */}
      <h1 style={{ fontSize: '22px', fontWeight: 700, color: '#dededa', letterSpacing: '-0.3px' }}>
        Profile
      </h1>
      <p style={{ fontSize: '13px', color: '#8c8b84', marginTop: '3px', marginBottom: '20px' }}>
        Manage your account settings
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', alignItems: 'start' }}>

        {/* Left — Edit profile */}
        <div className="stat-card">
          {/* Avatar row */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '20px' }}>
            <div
              className="avatar"
              style={{
                width: '64px', height: '64px', fontSize: '22px', flexShrink: 0,
                background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)'
              }}
            >
              AM
            </div>
            <div>
              <p style={{ fontSize: '17px', fontWeight: 700, color: '#dededa' }}>{userData.name}</p>
              <p style={{ fontSize: '12px', color: '#8c8b84', marginTop: '2px' }}>{userData.userId}</p>
            </div>
          </div>

          <form onSubmit={handleSubmit}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>Full name</label>
                <input type="text" className="input-field" defaultValue={userData.name} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>Email address</label>
                <input type="email" className="input-field" placeholder="amrit@example.com" />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>Phone number</label>
                <input type="tel" className="input-field" placeholder="+91 98765 43210" />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>Date of birth</label>
                <input type="date" className="input-field" />
              </div>
              <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '4px' }}>
                Update profile
              </button>
            </div>
          </form>
        </div>

        {/* Right column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>

          {/* Account stats */}
          <div className="stat-card">
            <p style={{ fontSize: '14px', fontWeight: 600, color: '#dededa', marginBottom: '4px' }}>
              Account statistics
            </p>
            <div>
              {statRow('Total transactions', userData.sentTransactions + userData.receivedTransactions)}
              {statRow('Money sent',     `₹${userData.totalSent.toLocaleString()}`,     '#f87171')}
              {statRow('Money received', `₹${userData.totalReceived.toLocaleString()}`, '#4ade80')}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '12px' }}>
                <span style={{ fontSize: '13.5px', color: '#8c8b84' }}>Current balance</span>
                <span style={{ fontSize: '16px', fontWeight: 700, fontFamily: "'DM Mono', monospace", color: '#4ade80' }}>
                  ₹{userData.balance.toLocaleString()}
                </span>
              </div>
            </div>
          </div>

          {/* Security */}
          <div className="stat-card">
            <p style={{ fontSize: '14px', fontWeight: 600, color: '#dededa', marginBottom: '10px' }}>
              Security settings
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {secBtn('Change password')}
              {secBtn('Enable two-factor authentication')}
              {secBtn('Manage devices')}
            </div>
          </div>

          {/* Preferences */}
          <div className="stat-card">
            <p style={{ fontSize: '14px', fontWeight: 600, color: '#dededa', marginBottom: '10px' }}>
              Preferences
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0' }}>
              {[
                { label: 'Email notifications',  defaultOn: true },
                { label: 'SMS notifications',    defaultOn: false },
                { label: 'Transaction alerts',   defaultOn: true },
              ].map((pref, i, arr) => (
                <div
                  key={pref.label}
                  style={{
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                    padding: '10px 0',
                    borderBottom: i < arr.length - 1 ? '1px solid #3a3a38' : 'none'
                  }}
                >
                  <span style={{ fontSize: '13.5px', color: '#dededa' }}>{pref.label}</span>
                  <Toggle defaultChecked={pref.defaultOn} />
                </div>
              ))}
            </div>
          </div>

        </div>
      </div>
    </div>
  )
}

export default Profile