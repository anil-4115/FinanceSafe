import { useEffect, useRef, useState } from 'react';
import { api } from '../services/api';

function AssistantPage() {
  const [messages, setMessages] = useState(() => [
    {
      role: 'assistant',
      content: 'Hi, I\'m the FinanceSafe assistant. Ask me about fraud, phishing, spending, saving or investing.',
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const scrollRef = useRef(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, loading]);

  async function send(event) {
    event.preventDefault();
    const text = input.trim();
    if (!text || loading) return;
    setInput('');
    setError('');
    setMessages((current) => [...current, { role: 'user', content: text }]);
    setLoading(true);
    try {
      const { data } = await api.post('/assistant/chat', { message: text });
      setMessages((current) => [...current, { role: 'assistant', content: data.reply }]);
      if (data.suggestedQuestions?.length) {
        setMessages((current) => [...current, { role: 'suggestions', content: data.suggestedQuestions }]);
      }
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not reach the assistant.');
    } finally {
      setLoading(false);
    }
  }

  function handleSuggestion(suggestion) {
    setInput(suggestion);
  }

  return (
    <div className="page-shell assistant-page">
      <h2>Assistant</h2>
      <div className="chat-shell" ref={scrollRef}>
        {messages.map((message, index) => {
          if (message.role === 'suggestions') {
            return (
              <div className="suggestion-row" key={index}>
                {message.content.map((question) => (
                  <button key={question} className="chip clickable" onClick={() => handleSuggestion(question)}>{question}</button>
                ))}
              </div>
            );
          }
          return (
            <div className={`chat-bubble ${message.role}`} key={index}>
              <span>{message.content}</span>
            </div>
          );
        })}
        {loading && <div className="chat-bubble assistant typing">…</div>}
        {error && <p className="form-error" role="alert">{error}</p>}
      </div>
      <form className="chat-form" onSubmit={send}>
        <input value={input} onChange={(event) => setInput(event.target.value)} placeholder="Try: is this payment link safe?" />
        <button className="primary-btn" disabled={loading}>Send</button>
      </form>
      <p className="muted chat-hint">Try: "is this OTP request a scam", "what is phishing", "should I invest in a crypto whatsapp group".</p>
    </div>
  );
}

export default AssistantPage;