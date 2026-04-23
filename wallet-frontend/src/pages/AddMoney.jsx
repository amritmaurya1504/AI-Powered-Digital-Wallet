import React, { useState } from 'react'

function AddMoney() {
  const [amount, setAmount] = useState('')
  const [method, setMethod] = useState('card')

  const handleSubmit = (e) => {
    e.preventDefault()
    alert(`Adding ₹${amount} via ${method}`)
  }

  const methodBtn = (id, label, sub) => (
    <button
      type="button"
      onClick={() => setMethod(id)}
      style={{
        padding: '12px 8px',
        borderRadius: '8px',
        border: `1px solid ${method === id ? '#2d5a8e' : '#3a3a38'}`,
        background: method === id ? '#1e3a5f' : '#2a2a28',
        color: method === id ? '#93c5fd' : '#8c8b84',
        cursor: 'pointer',
        transition: 'all 0.15s',
        textAlign: 'center',
        fontSize: '13px',
        fontWeight: 600,
        fontFamily: 'inherit'
      }}
    >
      <p style={{ marginBottom: '2px' }}>{label}</p>
      <p style={{ fontSize: '11px', opacity: 0.65, fontWeight: 400 }}>{sub}</p>
    </button>
  )

  return (
    <div>
      <h1 style={{ fontSize: '22px', fontWeight: 700, color: '#dededa', letterSpacing: '-0.3px' }}>
        Add money
      </h1>
      <p style={{ fontSize: '13px', color: '#8c8b84', marginTop: '3px', marginBottom: '20px' }}>
        Top up your wallet
      </p>

      <div style={{ maxWidth: '560px', display: 'flex', flexDirection: 'column', gap: '14px' }}>

        <div className="stat-card2">
          <p style={{ fontSize: '14px', fontWeight: 600, color: '#dededa', marginBottom: '16px' }}>
            Add funds
          </p>

          <form onSubmit={handleSubmit}>
            {/* Amount */}
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>
                Amount to add
              </label>
              <div style={{ position: 'relative' }}>
                <span style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#8c8b84' }}>₹</span>
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

            {/* Payment method */}
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '8px' }}>
                Payment method
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: '8px' }}>
                {methodBtn('card', 'Card',  'Credit/Debit')}
                {methodBtn('upi',  'UPI',   'Google Pay')}
                {methodBtn('bank', 'Bank',  'Net Banking')}
              </div>
            </div>

            {/* Card fields */}
            {method === 'card' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '16px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>Card number</label>
                  <input type="text" className="input-field" placeholder="1234 5678 9012 3456" />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>Expiry date</label>
                    <input type="text" className="input-field" placeholder="MM/YY" />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>CVV</label>
                    <input type="text" className="input-field" placeholder="123" />
                  </div>
                </div>
              </div>
            )}

            {method === 'upi' && (
              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '12px', color: '#8c8b84', marginBottom: '6px' }}>UPI ID</label>
                <input type="text" className="input-field" placeholder="yourname@upi" />
              </div>
            )}

            {method === 'bank' && (
              <div style={{
                marginBottom: '16px', padding: '12px 14px',
                background: '#2a2a28', borderRadius: '8px',
                border: '1px solid #3a3a38'
              }}>
                <p style={{ fontSize: '13px', color: '#8c8b84' }}>
                  You will be redirected to your bank's website to complete the transaction.
                </p>
              </div>
            )}

            <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
              Add ₹{amount || '0'}
            </button>
          </form>
        </div>

        {/* Suggested amounts */}
        <div className="stat-card2">
          <p style={{ fontSize: '14px', fontWeight: 600, color: '#dededa', marginBottom: '12px' }}>
            Suggested amounts
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: '8px' }}>
            {[500, 1000, 2000, 5000].map(amt => (
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
  )
}

export default AddMoney