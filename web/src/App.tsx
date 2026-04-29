import { useQuery } from '@tanstack/react-query'
import { useAuth } from './auth/AuthProvider'
import roastersApi from './services/roastersApi'

function App() {
  const { authenticated, loading, login, logout } = useAuth()

  const { data, isLoading: dataLoading, error } = useQuery({
    queryKey: ['roasters'],
    queryFn: () => roastersApi.list(),
    enabled: authenticated,
  })

  return (
    <div style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
      <header style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '2rem' }}>
        <h1 style={{ margin: 0 }}>Batch Hawk</h1>
        {!loading && (
          authenticated
            ? <button onClick={logout}>Log out</button>
            : <button onClick={login}>Log in</button>
        )}
      </header>

      {loading && <p>Loading...</p>}

      {!loading && !authenticated && (
        <p>Log in to browse roasters and products.</p>
      )}

      {authenticated && (
        <section>
          <h2>Roasters</h2>
          {dataLoading && <p>Loading roasters...</p>}
          {error && <p>Error loading roasters.</p>}
          {data && (
            <ul>
              {data.content?.map((r) => (
                <li key={r.id}>{r.name}</li>
              ))}
              {data.content?.length === 0 && <li>No roasters found.</li>}
            </ul>
          )}
        </section>
      )}
    </div>
  )
}

export default App
