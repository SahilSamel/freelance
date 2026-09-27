/**
 * Freelance Frontend Framework - Mock Data Provider
 * Pre-seeded mock dataset corresponding directly to the FitPulse UI design specifications.
 */

export const MockMembers = [
  {
    id: 'MEM-2024-092',
    name: 'Arjun Sharma',
    phone: '+91 98765 43210',
    email: 'arjun.sharma@gmail.com',
    plan: 'Annual Gold',
    totalPaid: 18500,
    totalPaidFormatted: '₹18,500',
    status: 'active',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=120',
    joinDate: '12 Jan 2024',
    expiryDate: '11 Jan 2025',
    paidAmount: '₹18,500 Paid',
    paymentMethod: 'via UPI • Txn #UPI8892',
    paymentDate: '12 Jan 2024'
  },
  {
    id: 'MEM-2024-093',
    name: 'Priya Patel',
    phone: '+91 87654 32109',
    email: 'priya.patel@yahoo.com',
    plan: 'Monthly Basic',
    totalPaid: 2500,
    totalPaidFormatted: '₹2,500',
    status: 'expiring',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=120',
    joinDate: '01 Aug 2024',
    expiryDate: '30 Sep 2024',
    paidAmount: '₹2,500 Paid',
    paymentMethod: 'via Card • Txn #CRD1022',
    paymentDate: '01 Aug 2024'
  },
  {
    id: 'MEM-2024-094',
    name: 'Rohan Verma',
    phone: '+91 76543 21098',
    email: 'rohan.v@outlook.com',
    plan: 'Quarterly Pro',
    totalPaid: 7200,
    totalPaidFormatted: '₹7,200',
    status: 'active',
    avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=120',
    joinDate: '15 Jul 2024',
    expiryDate: '15 Oct 2024',
    paidAmount: '₹7,200 Paid',
    paymentMethod: 'via NetBanking • Txn #NB8391',
    paymentDate: '15 Jul 2024'
  },
  {
    id: 'MEM-2024-095',
    name: 'Ananya Iyer',
    phone: '+91 65432 10987',
    email: 'ananya.iyer@gmail.com',
    plan: 'Annual Gold',
    totalPaid: 18500,
    totalPaidFormatted: '₹18,500',
    status: 'overdue',
    avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=120',
    joinDate: '10 Jan 2023',
    expiryDate: '09 Jan 2024',
    paidAmount: '₹18,500 Paid',
    paymentMethod: 'via UPI • Txn #UPI3920',
    paymentDate: '10 Jan 2023'
  },
  {
    id: 'MEM-2024-096',
    name: 'Kabir Singh',
    phone: '+91 95432 87654',
    email: 'kabir.singh@gmail.com',
    plan: 'Half-Yearly Elite',
    totalPaid: 11000,
    totalPaidFormatted: '₹11,000',
    status: 'expired',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=120',
    joinDate: '05 Jan 2024',
    expiryDate: '05 Jul 2024',
    paidAmount: '₹11,000 Paid',
    paymentMethod: 'via UPI • Txn #UPI5512',
    paymentDate: '05 Jan 2024'
  },
  {
    id: 'MEM-2024-097',
    name: 'Meera Nair',
    phone: '+91 85432 76543',
    email: 'meera.nair@hotmail.com',
    plan: 'Monthly Basic',
    totalPaid: 2500,
    totalPaidFormatted: '₹2,500',
    status: 'active',
    avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=120',
    joinDate: '10 Sep 2024',
    expiryDate: '10 Oct 2024',
    paidAmount: '₹2,500 Paid',
    paymentMethod: 'via Card • Txn #CRD9012',
    paymentDate: '10 Sep 2024'
  },
  {
    id: 'MEM-2024-098',
    name: 'Vikram Malhotra',
    phone: '+91 75432 65432',
    email: 'vikram.m@gmail.com',
    plan: 'Quarterly Pro',
    totalPaid: 7200,
    totalPaidFormatted: '₹7,200',
    status: 'active',
    avatar: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&q=80&w=120',
    joinDate: '20 Aug 2024',
    expiryDate: '20 Nov 2024',
    paidAmount: '₹7,200 Paid',
    paymentMethod: 'via UPI • Txn #UPI6641',
    paymentDate: '20 Aug 2024'
  },
  {
    id: 'MEM-2024-099',
    name: 'Neha Gupta',
    phone: '+91 65432 54321',
    email: 'neha.gupta@gmail.com',
    plan: 'Annual Gold',
    totalPaid: 18500,
    totalPaidFormatted: '₹18,500',
    status: 'active',
    avatar: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=120',
    joinDate: '01 Jan 2024',
    expiryDate: '31 Dec 2024',
    paidAmount: '₹18,500 Paid',
    paymentMethod: 'via UPI • Txn #UPI1129',
    paymentDate: '01 Jan 2024'
  }
];

export const MockConversations = [
  {
    id: 'conv_1',
    memberId: 'MEM-2024-092',
    name: 'Arjun Sharma',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=120',
    online: true,
    lastMessage: 'Hey, can you help me freeze my plan fo...',
    time: '11:42 AM',
    unreadCount: 2,
    messages: [
      {
        id: 'm1',
        sender: 'user',
        text: 'Hello, I just saw my membership is expiring in 2 days. Can you send me the renewal link?',
        time: '10:14 AM',
        isOutgoing: false
      },
      {
        id: 'm2',
        sender: 'staff',
        text: 'Hi Arjun! Sure, here is your renewal link for the *Annual Gold Plan* (₹18,500): contra.pay/fitpulse/renewal?id=9928',
        time: '10:15 AM',
        isOutgoing: true
      },
      {
        id: 'm3',
        sender: 'user',
        text: 'Hey, can you help me freeze my plan for next week as I will be traveling out of town?',
        time: '11:42 AM',
        isOutgoing: false
      }
    ]
  },
  {
    id: 'conv_2',
    memberId: 'MEM-2024-093',
    name: 'Priya Patel',
    avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=120',
    online: false,
    lastMessage: 'Sent the payment screenshot of ₹2,50...',
    time: '10:15 AM',
    unreadCount: 0,
    messages: [
      {
        id: 'm201',
        sender: 'user',
        text: 'Hi, I just transferred ₹2,500 for the monthly basic plan renewal.',
        time: '10:14 AM',
        isOutgoing: false
      },
      {
        id: 'm202',
        sender: 'user',
        text: 'Sent the payment screenshot of ₹2,500 via UPI.',
        time: '10:15 AM',
        isOutgoing: false
      }
    ]
  },
  {
    id: 'conv_3',
    memberId: 'MEM-2024-095',
    name: 'Ananya Iyer',
    avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=120',
    online: true,
    lastMessage: 'Will come by tomorrow evening to clea...',
    time: 'Yesterday',
    unreadCount: 0,
    messages: [
      {
        id: 'm301',
        sender: 'staff',
        text: 'Hello Ananya, this is a gentle reminder regarding your overdue membership fee of ₹18,500.',
        time: 'Yesterday',
        isOutgoing: true
      },
      {
        id: 'm302',
        sender: 'user',
        text: 'Will come by tomorrow evening to clear the overdue amount directly at the desk!',
        time: 'Yesterday',
        isOutgoing: false
      }
    ]
  },
  {
    id: 'conv_4',
    memberId: 'MEM-2024-096',
    name: 'Kabir Singh',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=120',
    online: false,
    lastMessage: 'Do you have personal trainer slots ope...',
    time: 'Yesterday',
    unreadCount: 0,
    messages: [
      {
        id: 'm401',
        sender: 'user',
        text: 'Do you have personal trainer slots open for 7 AM morning batches?',
        time: 'Yesterday',
        isOutgoing: false
      }
    ]
  },
  {
    id: 'conv_5',
    memberId: 'MEM-2024-097',
    name: 'Meera Nair',
    avatar: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=120',
    online: false,
    lastMessage: 'Thank you for the diet chart!',
    time: 'Oct 24',
    unreadCount: 0,
    messages: [
      {
        id: 'm501',
        sender: 'user',
        text: 'Thank you for the diet chart! Started it today.',
        time: 'Oct 24',
        isOutgoing: false
      }
    ]
  }
];

export const QuickTemplatesList = [
  {
    key: 'RENEWAL_REMINDER',
    title: 'Renewal Reminder',
    text: 'Hi {{name}}! Your gym membership is expiring on {{expiryDate}}. Click here to renew your plan: {{link}}'
  },
  {
    key: 'OVERDUE_NOTICE',
    title: 'Overdue Fee Notice',
    text: 'Dear {{name}}, your membership fee of {{amount}} is overdue. Please complete your payment to continue access: {{link}}'
  },
  {
    key: 'FESTIVE_OFFER',
    title: 'Festive Upgrade Offer',
    text: 'Special Offer for {{name}}! Upgrade to Annual Gold this week and get 2 extra months free!'
  },
  {
    key: 'WELCOME_ONBOARDING',
    title: 'Welcome to FitPulse',
    text: 'Welcome to FitPulse, {{name}}! Your locker and trainer allocation details have been activated.'
  }
];
