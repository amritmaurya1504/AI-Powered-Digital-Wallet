import React, { useState } from 'react'
import { Routes, Route } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import Dashboard from './pages/Dashboard'
import SendMoney from './pages/SendMoney'
import AddMoney from './pages/AddMoney'
import Transactions from './pages/Transactions'
import Profile from './pages/Profile'

function App() {
  return (
    <div className="app-container">
      <Sidebar />
      <div className="main-content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/send" element={<SendMoney />} />
          <Route path="/add" element={<AddMoney />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/profile" element={<Profile />} />
        </Routes>
      </div>
    </div>
  )
}

export default App