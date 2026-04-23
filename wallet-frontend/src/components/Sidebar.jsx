import React from 'react'
import { NavLink } from 'react-router-dom'
import { DashboardIcon, SendIcon, AddIcon, TransactionIcon, ProfileIcon } from './Icons'

const userData = {
  name: "Amrit",
  userId: "user-001"
}

function Sidebar() {
  return (
    <div className="sidebar">

      {/* Brand */}
      <div style={{ padding: '0 0.5rem 1.5rem' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 700, color: '#fbfbfb', letterSpacing: '-0.3px' }}>
          PayWallet
        </h2>
        <p style={{ fontSize: '13px', color: '#787670', marginTop: '2px' }}>
          Digital wallet
        </p>
      </div>

      {/* Nav */}
      <nav style={{ flex: 1 }}>
        <NavLink to="/" end className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <DashboardIcon />
          <span>Dashboard</span>
        </NavLink>
        <NavLink to="/send" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <SendIcon />
          <span>Send money</span>
        </NavLink>
        <NavLink to="/add" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <AddIcon />
          <span>Add money</span>
        </NavLink>
        <NavLink to="/transactions" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <TransactionIcon />
          <span>Transactions</span>
        </NavLink>
        <NavLink to="/profile" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <ProfileIcon />
          <span>Profile</span>
        </NavLink>
      </nav>

      {/* User */}
      <div style={{
        marginTop: 'auto',
        paddingTop: '1rem',
        borderTop: '1px solid #4e4e4e',
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        padding: '1rem 0.5rem 0'
      }}>
        <div className="avatar" style={{ background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)', width: '34px', height: '34px', fontSize: '11px', flexShrink: 0 }}>
          AM
        </div>
        <div>
          <p style={{ fontSize: '13px', fontWeight: 600, color: '#e2e8f0' }}>{userData.name}</p>
          <p style={{ fontSize: '11px', color: '#64748b' }}>{userData.userId}</p>
        </div>
      </div>

    </div>
  )
}

export default Sidebar
